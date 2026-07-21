# Selenium Automation Framework (Java 21)

Minimal, modular Selenium 4 + TestNG framework. Firefox is the default browser.

## Requirements
- Java 21
- Maven 3.9+
- Firefox installed (the driver binary is resolved automatically by Selenium Manager,
  built into Selenium 4.6+ — no WebDriverManager required)

## Run
```bash
mvn clean test
```

## qTest integration (automatic, no separate command)
`TestListener.onFinish` builds a JUnit-format XML report directly from the in-memory
TestNG results and uploads it to qTest's Automation API at the end of every
`mvn test` — nothing extra to run.

(It's built from live results rather than read back from Surefire's own
`target/surefire-reports/*.xml`, because Surefire only writes those files *after*
the whole TestNG run returns control to it — i.e. after `onFinish` has already run.
Reading them at that point would find nothing, or a stale file from the previous run.)

Enable it in `framework.properties`:
```properties
qtestEnabled=true
qtestDomain=yourcompany.qtestnet.com
qtestProjectId=12345
```
and export the API token as an environment variable — **never put it in
`framework.properties`**, since that file is committed to source control:
```bash
export QTEST_API_TOKEN=<your-token>
```

When disabled (the default) or when `QTEST_API_TOKEN` isn't set, the upload step
does nothing and logs why — it never breaks the test run itself. A local copy of
every generated report is also kept under `target/qtest-reports/` regardless of
whether the upload succeeds.

### The Extent HTML report is also attached (best-effort)

qTest's `auto-test-logs` endpoint only understands JUnit XML — it can't ingest an
arbitrary HTML file as "results". So getting the Extent report into qTest is a
second, separate step, chained automatically after the first:

1. Upload the JUnit XML → qTest treats this as an **asynchronous job** and returns
   a job id, which `QTestUploader` polls (`GET .../jobs/{jobId}`) until it reports
   `SUCCESS` or `FAILURE`.
2. On success, the job's response is searched for the created **Test Run id**.
3. The Extent HTML report (`ExtentReportManager.getReportPath()`) is then POSTed to
   that Test Run as a file attachment (`.../test-runs/{id}/blob-handles`).

**This was built without a live qTest account, so step 2/3 are a best guess at the
response shape** — every request/response is logged in full at each stage. If the
Test Run id can't be found (logged as a warning) or the attachment call is
rejected, share the logged job response and I'll adjust `findTestRunId()` or the
attachment URL to match your tenant's actual behaviour. Nothing here can break the
test run itself — every failure just logs and returns.

## Parallel execution
`testng.xml` runs with `parallel="methods" thread-count="3"` — each `@Test` method
runs on its own thread, launching its own browser instance.

The framework is built for this out of the box:
- `DriverManager` (`ThreadLocal<WebDriver>`), `ExtentReportManager`
  (`ThreadLocal<ExtentTest>`), `BaseTest.customerData` (`ThreadLocal<CustomerData>`),
  and `UIAssertions`' soft-assert collector are all thread-local, since TestNG runs a
  class's `@Test` methods on separate threads against **one shared instance** of that
  class under `parallel="methods"`. A plain instance field would be overwritten across
  threads; a `ThreadLocal` isn't.
- Tune `thread-count` to your machine's CPU/RAM — each thread opens its own browser.
  Consider `headless=true` in `framework.properties` when running many threads.
- To make an *individual* test class single-threaded relative to others, TestNG also
  supports `@Test(singleThreaded = true)` and per-class overrides if needed.

## Retrying failed tests
Retries are automatic and configurable — test classes never declare a retry analyzer
themselves. `RetryTransformer` (registered via `@Listeners` on `BaseTest`) attaches
`RetryAnalyzer` to every `@Test` method; `RetryAnalyzer` reads its behaviour from
`framework.properties`:

```properties
retryEnabled=true   # turn retries on/off
retryCount=2        # max retry attempts after the first failure
```

Set `retryEnabled=false` to disable retries entirely, or tune `retryCount` per run.
Each retry re-invokes `@BeforeMethod`/`@AfterMethod` in full, so it gets a fresh
browser and a fresh Extent report node — useful for genuinely flaky UI steps, not a
substitute for fixing a consistently failing test.

## Test categories (groups)
Tag tests with TestNG `groups` and run only a category — the selected tests still run
in parallel per the config above.

```java
@Test(groups = {"smoke", "regression"})
public void verifyLogin() { ... }
```

Run a category from the command line:
```bash
mvn test -Dgroups=smoke              # only tests tagged "smoke"
mvn test -Dgroups=smoke,regression   # either group
mvn test -DexcludedGroups=regression # everything except "regression"
mvn test                             # no filter — runs everything (default)
```

This is wired through `pom.xml`'s Surefire config (`<groups>${groups}</groups>`,
`<excludedGroups>${excludedGroups}</excludedGroups>`), which reads the `-Dgroups` /
`-DexcludedGroups` system properties and applies them on top of `testng.xml` — no
per-category suite file needed. A test with no group runs unless a filter is passed.

## Reporting
- **Extent report:** `target/extent-report/<timestamp>/index.html` — each run gets its
  own timestamped folder. Every step is logged with a screenshot, and a final pass/fail
  screenshot is attached at the end of each test.
- The `TestListener` is registered via `@Listeners` on `BaseTest`, so the report is
  produced whether tests run from the IDE or from `testng.xml`.
- **Logs:** console + `target/logs/automation.log` (same steps mirrored via SLF4J/Logback).
- **Screenshots on disk:** `target/screenshots/`.

## Configuration
Edit `src/main/resources/framework.properties`:
```properties
browser=firefox            # firefox | chrome | edge
headless=false
url=https://example.com
implicitWait=10
explicitWait=20
screenshotOnEachStep=true  # set false to only screenshot on pass/fail
dataSource=json            # json | excel — how test data is loaded

# Database (SQL) — drop your JDBC driver on the classpath and set these
db.url=jdbc:mysql://localhost:3306/testdb
db.username=root
db.password=root
```

## Structure
```
framework
├── base        DriverContext, BaseTest, BaseUITest, BasePage, DriverManager
├── pages       Page Objects (LoginPage)
├── assertions  UIAssertions (hard + soft)
├── api         executors/ + validators/ (REST Assured)
├── reporting   ExtentReportManager, TestListener
├── retry       RetryAnalyzer, RetryTransformer
├── qtest       QTestUploader
├── utilities   PropertyManager, WaitUtils, ScreenshotUtils, JsonUtils, ExcelUtils, StepLogger, LoggerUtil
├── data
│   ├── customer     CustomerData (test-data POJO)
│   ├── database     DatabaseConnection, DatabaseValidator (SQL/JDBC)
│   ├── environment  (placeholder)
│   └── pojo         (placeholder)
└── constants   FrameworkConstants

src/test/resources/testdata/<testMethodName>.json   # test data, named after the test
```

## Key conventions
- Driver is held in a `ThreadLocal` inside `DriverManager` — no singleton.
- `DriverContext` is the shared ancestor that exposes `getDriver()`; both `BaseTest`
  and `BasePage` extend it, so tests **and** pages inherit the driver without pages
  becoming tests (no TestNG annotations leak into Page Objects).
- Page Objects extend `BasePage` and obtain the driver via inheritance; **no** constructor injection.
- `By` locators only — no PageFactory, no static sleeps.

### Test lifecycle
- `BaseUITest` **launches the app automatically** before every test — test classes write
  only `@Test` methods, no `@BeforeMethod` of their own.
- A test class may set a custom launch URL from its constructor: `super("https://…")`.
  Omit the constructor to use the URL from `framework.properties`.
- The constructor only *configures* the URL — the driver is created per test in
  `BaseTest.setUp`, so navigation itself runs in `BaseUITest`'s `@BeforeMethod` (a driver
  cannot be used from a constructor, which runs once, before any driver exists).

### Assertions
- Tests **never** call raw `Assert.*`. All UI assertions go through `UIAssertions`,
  exposed as `assertions` on `BaseUITest`.
- Both **hard** (`assertTrue`, `assertEquals`, …) and **soft**
  (`softAssertTrue`, `softAssertEquals`, …) assertions are supported.
- Soft assertions are collected per thread and **finalised automatically** by
  `TestListener` after each test — tests do not call `assertAll()`.
- Database validations live in their own class, `data.database.DatabaseValidator`.

### Test data (JSON or Excel)
- The `dataSource` property toggles the source: `json` or `excel`. The rest of the flow
  is identical.
- The data file is **named after the test method**: `testdata/<testMethodName>.json`
  or `testdata/<testMethodName>.xlsx`.
- Attribute names match the `CustomerData` fields — JSON keys, or **Excel headers**
  (row 0 = headers, row 1 = data).
- `BaseTest` loads it via `JsonUtils` / `ExcelUtils` and stores it in `customerData`
  (a `ThreadLocal<CustomerData>`) **before** the test runs — tests call
  `customerData.get()`, never parse files themselves.
- If no matching file exists, `customerData.get()` returns an empty (non-null) object.
- Only the resolved `CustomerData` object (the result of `.get()`) is passed around —
  e.g. into a Page Object method (`loginPage.login(customerData.get())`). From there
  it's an ordinary object reference: if a Page Object method needs to forward it to
  another method or another Page Object, it's just a normal method parameter, no
  `ThreadLocal` involved beyond that one `.get()` call in the test.
