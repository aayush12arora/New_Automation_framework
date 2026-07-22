# Selenium Automation Framework (Java 21)

A minimal but production-shaped UI + API automation framework built on **Java 21,
Selenium 4, TestNG and Maven**. It is deliberately small — no Cucumber, no Spring, no
Lombok, no dependency-injection container — but it demonstrates every concern a real
framework has to solve: thread-safe driver management, the Page Object Model, data-driven
testing, rich reporting, automatic screenshots, configurable retries, parallel execution,
test categorisation, database/API validation, and results publishing to qTest.

> This README is intentionally exhaustive. It explains **what every file does**, **how
> data flows through a run**, **every annotation used**, and **how the reporting and retry
> pieces are wired in**. The last section lists **30 questions** you might be asked about
> it, with answers.

---

## Table of contents

1. [Technology stack](#1-technology-stack)
2. [How to run](#2-how-to-run)
3. [Directory structure — file by file](#3-directory-structure--file-by-file)
4. [The end-to-end execution flow (most important)](#4-the-end-to-end-execution-flow)
5. [Every annotation used, explained](#5-every-annotation-used-explained)
6. [Deep dives](#6-deep-dives)
   - [6.1 Driver management (ThreadLocal, no singleton)](#61-driver-management)
   - [6.2 The inheritance model](#62-the-inheritance-model)
   - [6.3 Data-driven testing (JSON / Excel toggle)](#63-data-driven-testing)
   - [6.4 Assertions (hard + soft, auto-finalised)](#64-assertions)
   - [6.5 Reporting integration (Extent + logs + screenshots)](#65-reporting-integration)
   - [6.6 Retry integration](#66-retry-integration)
   - [6.7 Parallel execution & thread-safety](#67-parallel-execution--thread-safety)
   - [6.8 Test categories (groups)](#68-test-categories-groups)
   - [6.9 qTest integration](#69-qtest-integration)
   - [6.10 Database & API validation](#610-database--api-validation)
7. [Configuration reference (`framework.properties`)](#7-configuration-reference)
8. [30 questions you could be asked (with answers)](#8-30-questions-you-could-be-asked)

---

## 1. Technology stack

| Concern | Library | Notes |
|---|---|---|
| Language | **Java 21** | Uses `switch` expressions, text `formatted()`, `var`, try-with-resources. |
| Build | **Maven** | Surefire runs the suite via `testng.xml`. |
| Browser automation | **Selenium 4** | Driver binaries resolved by **Selenium Manager** (built into 4.6+) — no WebDriverManager. |
| Test runner | **TestNG** | Lifecycle annotations, listeners, groups, parallel execution, retry analyzer. |
| Reporting | **ExtentReports (Spark)** | HTML report with per-step screenshots. |
| Logging | **SLF4J + Logback** | Console + `target/logs/automation.log`. |
| JSON / object mapping | **Jackson** | JSON test data → POJO, and Excel `Map` → POJO. |
| Excel test data | **Apache POI** (`poi-ooxml`) | `.xlsx` reading. |
| API testing | **REST Assured** | Sample executor + validator. |
| File/utility | **Apache Commons IO / Lang3** | Screenshot file copy, etc. |
| POI logging bridge | **log4j-to-slf4j** | Routes POI's Log4j2 logging into Logback. |

Everything is pinned to Java-21-compatible stable versions in `pom.xml`.

---

## 2. How to run

```bash
mvn clean test                       # run everything (uses testng.xml)
mvn test -Dgroups=smoke              # run only the "smoke" category
mvn test -Dgroups=smoke,regression   # either group
mvn test -DexcludedGroups=regression # everything except "regression"
```

Change the browser, waits, data source, retries etc. in
`src/main/resources/framework.properties` — **no code change needed**.

Outputs after a run:
- **Extent report:** `target/extent-report/<timestamp>/index.html`
- **Logs:** console + `target/logs/automation.log`
- **Screenshots:** `target/screenshots/`
- **qTest upload XML (local copy):** `target/qtest-reports/`

---

## 3. Directory structure — file by file

```
src/main/java/framework/
├── base/
│   ├── DriverContext.java     Shared parent that exposes getDriver(); extended by BaseTest AND BasePage
│   ├── DriverManager.java     Owns the ThreadLocal<WebDriver>; setDriver/getDriver/quitDriver; browser factory
│   ├── BaseTest.java          Root test class: @BeforeMethod/@AfterMethod, loads test data, registers listeners
│   ├── BaseUITest.java        Adds auto-launch of the app + the shared UIAssertions instance
│   └── BasePage.java          Parent of all Page Objects: logged Selenium wrappers (click, type, wait, …)
├── pages/
│   └── LoginPage.java         Sample Page Object: locators + business methods only
├── assertions/
│   └── UIAssertions.java      Hard + soft assertion helpers; ThreadLocal<SoftAssert>; static assertAll()/reset()
├── reporting/
│   ├── ExtentReportManager.java  Single ExtentReports + ThreadLocal<ExtentTest>; timestamped report path
│   └── TestListener.java      ITestListener + IInvokedMethodListener: soft-assert finalise, screenshots, flush, qTest
├── retry/
│   ├── RetryAnalyzer.java     IRetryAnalyzer: re-runs a failed @Test up to retryCount (config-gated)
│   └── RetryTransformer.java  IAnnotationTransformer: auto-attaches RetryAnalyzer to every @Test
├── qtest/
│   └── QTestUploader.java     Builds JUnit XML from live results, uploads to qTest, attaches the Extent report
├── utilities/
│   ├── PropertyManager.java   Loads framework.properties once; get/getBoolean/getInt
│   ├── WaitUtils.java         Explicit waits (visibility / clickability) — no static sleeps
│   ├── ScreenshotUtils.java   PNG-to-disk + Base64-for-report capture
│   ├── JsonUtils.java         Jackson read/convert/resourceExists helpers
│   ├── ExcelUtils.java        POI .xlsx reader → header→value Map
│   ├── StepLogger.java        One call that logs to SLF4J AND the Extent report (with screenshot)
│   └── LoggerUtil.java        Thin SLF4J LoggerFactory wrapper
├── data/
│   ├── customer/CustomerData.java   Test-data POJO (username, password, firstName, lastName, email)
│   ├── database/DatabaseConnection.java  Opens JDBC connections from properties
│   ├── database/DatabaseValidator.java   querySingleValue / recordExists / rowCount
│   ├── environment/          (placeholder for environment configs)
│   └── pojo/                 (placeholder for other POJOs)
├── api/
│   ├── executors/ApiExecutor.java   REST Assured GET/POST wrappers
│   └── validators/ApiValidator.java hasStatus / bodyContains
└── constants/
    └── FrameworkConstants.java  All property keys and paths in one place

src/main/resources/
├── framework.properties      All runtime configuration
└── logback.xml               Console + file logging config

src/test/java/tests/
└── LoginTest.java            Sample test: extends BaseUITest, uses customerData + assertions

src/test/resources/testdata/
├── verifyLogin.json          JSON test data (file name == test method name)
└── verifyLogin.xlsx          Excel test data (same data; used when dataSource=excel)

testng.xml                    Suite: parallel="methods" thread-count="3"
pom.xml                       Dependencies + Surefire config (suite file + -Dgroups wiring)
```

---

## 4. The end-to-end execution flow

This is the single most useful thing to understand. When you run `mvn test`, here is the
exact order of what fires for the sample `LoginTest.verifyLogin` (annotations in **bold**):

```
mvn test
  └─ Surefire reads pom.xml → runs testng.xml (parallel="methods", thread-count=3)

  ── ONCE, at annotation-reading time ──────────────────────────────────────────
  RetryTransformer.transform(...)          ← @Listeners on BaseTest registers it
      → annotation.setRetryAnalyzer(RetryAnalyzer.class)   for EVERY @Test method

  ── PER @Test METHOD, on its own thread ───────────────────────────────────────
  1. @BeforeMethod  BaseTest.setUp(Method)          (superclass runs first)
       ├─ ExtentReportManager.createTest("LoginTest.verifyLogin")   → thread's report node
       ├─ customerData.set(loadTestData("verifyLogin"))             → JSON or Excel → POJO
       ├─ StepLogger.step("=== initialising driver ===")
       └─ DriverManager.setDriver()                                 → ThreadLocal<WebDriver>

  2. @BeforeMethod  BaseUITest.launchApplication()  (subclass runs after)
       └─ getDriver().get(url)   (url from super("...") or framework.properties)

  3. @Test  LoginTest.verifyLogin()
       ├─ new LoginPage()                    (no driver passed in — inherits getDriver())
       ├─ page.type()/click()/...            → BasePage wrapper → WaitUtils + StepLogger
       │                                        (each step: SLF4J log + Extent + screenshot)
       └─ assertions.softAssert*(...)        → collected in ThreadLocal<SoftAssert>

  4. IInvokedMethodListener.afterInvocation()   (TestListener)
       ├─ if test passed so far:  UIAssertions.assertAll()
       │        └─ a soft-assert failure here FLIPS the result to FAILURE
       └─ if already failed:      UIAssertions.reset()  (discard collected soft asserts)

  5. ITestListener.onTestSuccess / onTestFailure / onTestSkipped   (TestListener)
       └─ attachFinalScreenshot()  → save PNG + embed Base64 in report + flush
       (If it FAILED, TestNG now consults RetryAnalyzer.retry() → maybe re-run from step 1)

  6. @AfterMethod  BaseTest.tearDown()
       ├─ StepLogger.step("=== quitting driver ===")
       ├─ DriverManager.quitDriver()          → driver.quit() + ThreadLocal.remove()
       ├─ ExtentReportManager.remove()         → clears the thread's report node
       └─ customerData.remove()                → clears the thread's data

  ── ONCE, after all methods ───────────────────────────────────────────────────
  7. ITestListener.onFinish()   (TestListener)
       ├─ ExtentReportManager.flush()          → writes the HTML report to disk
       └─ QTestUploader.uploadIfEnabled(ctx)   → JUnit XML → qTest (+ attach Extent), if enabled
```

The key insight is that **the test class itself is tiny** — all of the lifecycle
(driver, data, reporting, screenshots, retry, upload) is handled by base classes and
listeners, so a test author only writes locators, business methods, and assertions.

---

## 5. Every annotation used, explained

| Annotation | Where | What it does here |
|---|---|---|
| `@Test` | `LoginTest` | Marks a test method. `groups = {"smoke","regression"}` categorises it. |
| `@BeforeMethod(alwaysRun = true)` | `BaseTest.setUp`, `BaseUITest.launchApplication` | Runs before **every** `@Test`. `alwaysRun=true` means it still runs even if a group filter or a prior config would otherwise skip it. Superclass `@BeforeMethod` runs before subclass. |
| `@AfterMethod(alwaysRun = true)` | `BaseTest.tearDown` | Runs after every `@Test` (even on failure) to quit the driver and clear thread-locals. |
| `@Listeners({TestListener.class, RetryTransformer.class})` | `BaseTest` | Registers both listeners for every subclass, so they apply whether you run from the IDE or `testng.xml`. This is why `testng.xml` has no `<listeners>` block. |
| `@Override` | listeners | Standard override of TestNG interface methods. |
| `@JsonIgnoreProperties(ignoreUnknown = true)` | `CustomerData` | Jackson annotation — ignore JSON keys with no matching field, so one POJO can back many data files. |

TestNG **interfaces** implemented (not annotations, but part of the extension model):

| Interface | Implemented by | Purpose |
|---|---|---|
| `ITestListener` | `TestListener` | `onTestSuccess/Failure/Skipped`, `onFinish` — reporting hooks. |
| `IInvokedMethodListener` | `TestListener` | `afterInvocation` — finalise soft assertions right after the test body. |
| `IRetryAnalyzer` | `RetryAnalyzer` | `retry()` — decides whether a failed test re-runs. |
| `IAnnotationTransformer` | `RetryTransformer` | `transform()` — mutates the `@Test` annotation at load time to attach the retry analyzer. |

---

## 6. Deep dives

### 6.1 Driver management

`DriverManager` holds a `private static final ThreadLocal<WebDriver> DRIVER`. It exposes
exactly three lifecycle methods plus a guard:

- `setDriver()` — reads `browser`/`headless`/`implicitWait`, builds the driver in a Java 21
  `switch` expression (`firefox`/`chrome`/`edge`), sets implicit wait + maximises, and binds
  it to the current thread. **Selenium Manager** downloads the correct driver binary
  automatically, so there is no WebDriverManager dependency.
- `getDriver()` — returns the current thread's driver, or throws `IllegalStateException` if
  none is set (a clear failure instead of a `NullPointerException`).
- `quitDriver()` — quits and `remove()`s the driver from the thread.
- `isDriverInitialized()` — used by `ScreenshotUtils` so screenshots are skipped safely when
  no driver exists.

**Why `ThreadLocal` and not a singleton?** With `parallel="methods"`, several tests run at
once. A singleton would share one browser across threads and corrupt every session. A
`ThreadLocal` gives each thread its own isolated driver with no locking.

### 6.2 The inheritance model

```
DriverContext            getDriver()  (the ONLY place tests/pages reach the driver)
├── BaseTest             lifecycle + data + @Listeners   → BaseUITest → LoginTest
└── BasePage             logged Selenium wrappers          → LoginPage
```

`DriverContext` is a deliberate design choice. Both tests and Page Objects need the driver,
but a Page Object must **not** inherit the test lifecycle (`@BeforeMethod`, listeners). So
the driver accessor lives in a small shared parent that both branches extend. This is why:

- Page Objects are created with `new LoginPage()` — **no `WebDriver` in the constructor**
  (no constructor injection). They get the driver by inheriting `getDriver()`.
- Tests also call `getDriver()` from the same shared method — no duplication.

### 6.3 Data-driven testing

Test data is **never parsed inside a test**. `BaseTest.setUp` does it before the test runs:

1. `loadTestData(methodName)` reads the `dataSource` property (`json` or `excel`) via a
   Java 21 `switch`.
2. **JSON path:** `JsonUtils.read("testdata/verifyLogin.json", CustomerData.class)` — Jackson
   maps keys → fields.
3. **Excel path:** `ExcelUtils.readFirstRow("testdata/verifyLogin.xlsx")` returns a
   `header → value` `Map`, then `JsonUtils.convert(map, CustomerData.class)` maps it onto the
   same POJO via Jackson's `convertValue`.
4. The result is stored in `customerData` (a `ThreadLocal<CustomerData>`). Tests read it with
   **`customerData.get()`**.

The **file name equals the test method name** (`verifyLogin.json` / `verifyLogin.xlsx`), and
**JSON keys / Excel headers equal the `CustomerData` field names**. If no data file exists,
an empty (non-null) `CustomerData` is returned so `customerData.get()` never NPEs. Switching
JSON↔Excel is a one-line property change with no code impact.

### 6.4 Assertions

Tests never call raw `Assert.*`. All verification goes through `UIAssertions` (exposed as
`assertions` on `BaseUITest`), which offers **hard** (`assertTrue`, `assertEquals`,
`assertNotNull`, `assertContains`) and **soft** (`softAssert*`) variants. Every assertion is
also logged as a report step.

- **Hard** assertions throw immediately on failure — the test stops there.
- **Soft** assertions are collected in a `ThreadLocal<SoftAssert>` and verified **together at
  the end**, so a test can report several failures at once.

**The clever part:** tests do **not** call `assertAll()`. `TestListener.afterInvocation`
(the `IInvokedMethodListener` hook that fires right after the test body) calls
`UIAssertions.assertAll()` for a passing test. If a soft assertion failed, `assertAll()`
throws, and the listener flips the `ITestResult` to `FAILURE`. This means soft failures
become real failures **without any boilerplate in the test**, and — because it happens before
the pass/fail listener callbacks — the report and retry logic see the correct final result.

### 6.5 Reporting integration

Three pieces cooperate:

- **`ExtentReportManager`** owns a single `ExtentReports` instance (one HTML report per run,
  written to a **timestamped folder** `target/extent-report/<timestamp>/index.html`) and a
  `ThreadLocal<ExtentTest>` so each parallel test logs into its own report node.
  `createTest()` in `setUp`, `remove()` in `tearDown`, `flush()` writes to disk.
- **`StepLogger.step(log, message)`** is the single entry point every action calls. It:
  1. writes the message to **SLF4J** (so it appears in console + `automation.log`), and
  2. if a report node exists, adds the same message to the **Extent report** — with a
     **Base64 screenshot** attached when `screenshotOnEachStep=true`.
- **`BasePage` wrappers** (`click`, `type`, `waitForVisible`, …) each call `StepLogger.step`,
  so every UI action is automatically logged, reported, and screenshotted with no effort in
  the Page Object.

At the end of each test, `TestListener` attaches a **final pass/fail screenshot** (saved to
disk *and* embedded in the report) and flushes. On failure it also logs the throwable into
the report. Flushing after each test means the report survives an interrupted run.

### 6.6 Retry integration

Two classes, working together, make retries **automatic and configurable** without touching
any test:

- **`RetryAnalyzer implements IRetryAnalyzer`** — TestNG calls its `retry(ITestResult)` when a
  test fails. It returns `true` (re-run) only while `retryEnabled=true` **and** the per-method
  attempt counter is below `retryCount`. TestNG creates a fresh `RetryAnalyzer` per method, so
  the counter needs no synchronisation.
- **`RetryTransformer implements IAnnotationTransformer`** — its `transform()` runs once per
  `@Test` at annotation-reading time and calls `annotation.setRetryAnalyzer(RetryAnalyzer.class)`.
  This is what removes the need to write `@Test(retryAnalyzer = …)` on every method.

`RetryTransformer` is registered by the same `@Listeners` on `BaseTest`. So the flow is:
transformer attaches the analyzer to all tests → a test fails → TestNG asks the analyzer →
if config allows, the **whole method re-runs from `@BeforeMethod`** (fresh browser, fresh
report node, fresh data). Set `retryEnabled=false` to turn it all off.

### 6.7 Parallel execution & thread-safety

`testng.xml` uses `parallel="methods" thread-count="3"` — each `@Test` runs on its own
thread. Under this mode TestNG uses **one shared instance** of the test class across those
threads, so any plain instance field would be overwritten mid-test. Everything mutable that
is per-test is therefore **thread-local**:

| State | Holder |
|---|---|
| WebDriver | `DriverManager.DRIVER` (`ThreadLocal<WebDriver>`) |
| Extent report node | `ExtentReportManager.TEST` (`ThreadLocal<ExtentTest>`) |
| Test data | `BaseTest.customerData` (`ThreadLocal<CustomerData>`) |
| Soft-assert collector | `UIAssertions.SOFT` (`ThreadLocal<SoftAssert>`) |

`thread-count` should match the machine's CPU/RAM since each thread launches its own browser;
consider `headless=true` for high counts.

### 6.8 Test categories (groups)

Tag any test with `@Test(groups = {"smoke", "regression"})`. Run a category with
`mvn test -Dgroups=smoke` (or `-DexcludedGroups=…`). This is wired through Surefire in
`pom.xml` (`<groups>${groups}</groups>`), which reads the `-Dgroups` system property on top
of `testng.xml`. Adding a new category is just a new name in the annotation — no other config.
Group filtering and parallelism are independent, so a filtered subset still runs in parallel.

### 6.9 qTest integration

At the end of a run, `TestListener.onFinish` calls `QTestUploader.uploadIfEnabled(context)`.
When `qtestEnabled=true` it:

1. **Builds JUnit XML** directly from the in-memory `ITestContext` results (passed/failed/
   skipped). It builds it from live results rather than reading Surefire's
   `target/surefire-reports/*.xml`, because Surefire writes those files only **after** the
   TestNG run returns control — i.e. after `onFinish` has already executed.
2. **Uploads** the XML to qTest's `auto-test-logs` endpoint using Java's built-in
   `HttpClient` (no new dependency). That call returns an async **job id**.
3. **Polls** the job until it reports `SUCCESS`, then searches the response for the created
   **Test Run id** and **attaches the Extent HTML report** to it.

The token is read from the `QTEST_API_TOKEN` **environment variable** (never committed);
domain/project id/enable-flag are in `framework.properties`. If disabled or the token is
missing, the step logs why and does nothing — it never breaks the test run. A local copy of
the generated XML is kept under `target/qtest-reports/`.

> The attachment (step 3) is best-effort: it was written without a live qTest tenant, so the
> exact job-response shape and attachment endpoint may differ per version. Every request and
> response is logged in full so the field extraction can be corrected against real output.

### 6.10 Database & API validation

- **`DatabaseConnection.open()`** builds a JDBC `Connection` from `db.url/username/password`.
  It is driver-agnostic (pure `java.sql`), so dropping any JDBC driver on the classpath works.
- **`DatabaseValidator`** keeps SQL out of tests: `querySingleValue`, `recordExists`,
  `rowCount`, each using try-with-resources so connections always close.
- **`ApiExecutor`** wraps REST Assured `GET`/`POST`; **`ApiValidator`** offers `hasStatus`
  and `bodyContains`. These let a test mix UI and API steps (e.g. set up data via API, verify
  via UI, confirm via DB).

---

## 7. Configuration reference

`src/main/resources/framework.properties`:

```properties
browser=firefox            # firefox | chrome | edge
headless=false             # run the browser headless
url=https://example.com    # default launch URL (overridable per test via super(url))
implicitWait=10            # seconds — global implicit wait
explicitWait=20            # seconds — used by WaitUtils

screenshotOnEachStep=true  # screenshot every step into the Extent report (false = only pass/fail)

retryEnabled=true          # retry failed tests
retryCount=2               # max retries after the first failure

dataSource=json            # json | excel — how CustomerData is loaded

db.url=jdbc:mysql://localhost:3306/testdb   # SQL validation (needs a JDBC driver on classpath)
db.username=root
db.password=root

qtestEnabled=false                     # publish results to qTest at end of run
qtestDomain=yourcompany.qtestnet.com
qtestProjectId=12345
# QTEST_API_TOKEN is an ENVIRONMENT VARIABLE, never a property — keeps the token out of git
```

---

## 8. 30 questions you could be asked

**Architecture & design**

1. **Why did you use `ThreadLocal` for the driver instead of a singleton?**
   A singleton shares one browser across all threads; with `parallel="methods"` that corrupts
   concurrent sessions. `ThreadLocal` gives each thread its own driver with no locking, and
   `remove()` in teardown prevents leaks across the thread pool.

2. **Why doesn't `BasePage` take a `WebDriver` in its constructor?**
   To avoid constructor injection and keep Page Objects creatable with `new LoginPage()`.
   The driver is inherited from `DriverContext.getDriver()`, which pulls the current thread's
   driver from `DriverManager`.

3. **What is `DriverContext` for and why not just put `getDriver()` in `BaseTest`?**
   Both tests and Page Objects need the driver, but Page Objects must not inherit the test
   lifecycle (`@BeforeMethod`, listeners). `DriverContext` is a shared parent both extend, so
   pages get the driver without becoming tests. It also avoids duplicating the accessor.

4. **How does a Page Object get the driver if it's never passed one?**
   `BasePage extends DriverContext`, whose `getDriver()` returns `DriverManager.getDriver()`
   — the current thread's driver. So any wrapper method just calls `getDriver()`.

5. **Why no PageFactory / `@FindBy`?**
   PageFactory adds proxy magic and lazy `StaleElement` pitfalls. Plain `By` locators are
   explicit, easy to read, and work cleanly with the explicit-wait wrappers in `BasePage`.

6. **Where are waits handled and why no `Thread.sleep`?**
   In `WaitUtils`, using `WebDriverWait` + `ExpectedConditions` (visibility/clickability).
   Static sleeps are flaky and slow; condition-based waits return as soon as the state is met.
   There is one implicit wait set globally plus explicit waits for specific conditions.

7. **What is the difference between implicit and explicit wait here?**
   Implicit wait (`implicitWait`) is a global "poll for presence" applied to every element
   lookup, set once in `DriverManager`. Explicit wait (`explicitWait`) is condition-specific,
   applied by `WaitUtils` for visibility/clickability. We keep implicit low to avoid the two
   compounding.

**Data-driven**

8. **How is test data loaded, and where?**
   In `BaseTest.setUp`, before the test body, via `loadTestData(methodName)`. The `dataSource`
   property selects JSON or Excel. The result populates a `ThreadLocal<CustomerData>`.

9. **How do you switch between JSON and Excel?**
   Change `dataSource=json|excel` in `framework.properties`. No code changes — a `switch` in
   `loadTestData` picks the loader.

10. **How does Excel data end up in the same POJO as JSON?**
    `ExcelUtils.readFirstRow` returns a `header→value` Map; `JsonUtils.convert` uses Jackson's
    `convertValue` to map that Map onto `CustomerData`, exactly like it maps JSON. Headers must
    match field names.

11. **Why is the data file named after the test method?**
    Convention over configuration: `verifyLogin` → `testdata/verifyLogin.json`. `BaseTest`
    derives the file name from the method name, so no test has to specify its data file.

12. **What happens if a test has no data file?**
    `loadTestData` returns a fresh empty `CustomerData`, so `customerData.get()` is always
    non-null and the test never NPEs.

13. **Why is `customerData` a `ThreadLocal` field instead of a plain field?**
    Under `parallel="methods"` TestNG shares one test-class instance across threads; a plain
    field would be overwritten by another thread's `setUp`. `ThreadLocal` isolates it per thread.

**Assertions**

14. **Difference between hard and soft assertions in your framework?**
    Hard (`assertTrue`, etc.) fail immediately. Soft (`softAssert*`) collect failures in a
    `ThreadLocal<SoftAssert>` and report them all at the end, so one run can surface multiple
    problems.

15. **Tests never call `assertAll()` — how do soft assertions get verified?**
    `TestListener.afterInvocation` (an `IInvokedMethodListener` hook) calls
    `UIAssertions.assertAll()` after the test body. If it throws, the listener flips the
    `ITestResult` to `FAILURE`. So soft failures become real failures with zero boilerplate.

16. **Why put assertions in a dedicated `UIAssertions` class instead of in tests/pages?**
    Page Objects must contain no assertions (POM discipline), and tests should read as business
    steps. Centralising assertions also lets every assertion be logged/screenshotted uniformly.

**Reporting & logging**

17. **How is ExtentReports integrated — walk me through it.**
    `ExtentReportManager` holds one `ExtentReports` and a `ThreadLocal<ExtentTest>`. `setUp`
    calls `createTest`, every step calls `StepLogger` which logs to the node, teardown removes
    the node, and `onFinish` flushes to `target/extent-report/<timestamp>/index.html`.

18. **How do steps get into both the logs and the report with one call?**
    `StepLogger.step(log, msg)` writes to SLF4J *and* to the current Extent node (with a
    screenshot when enabled). Every `BasePage` wrapper calls it, so all actions are captured.

19. **How are screenshots captured and attached?**
    `ScreenshotUtils.captureBase64()` for inline report embedding (per step), and
    `capture()` writes a PNG to `target/screenshots/`. The final pass/fail shot is taken in
    `TestListener` and both saved and embedded.

20. **Why a timestamped report folder?**
    So each run's report is preserved instead of overwritten, which matters for history/CI.

21. **Why register listeners with `@Listeners` on `BaseTest` instead of in `testng.xml`?**
    So they apply no matter how tests are launched (IDE run, a different suite file, CI). It
    also keeps `testng.xml` minimal. The report was previously "not generating" precisely
    because it was only wired in `testng.xml` and IDE runs bypassed it.

**Retry**

22. **How is the retry analyzer integrated without editing every test?**
    `RetryTransformer` (an `IAnnotationTransformer`) calls `setRetryAnalyzer` on every `@Test`
    at load time. It's registered via `@Listeners` on `BaseTest`, so no test declares a retry
    analyzer.

23. **How is retry made configurable?**
    `RetryAnalyzer.retry()` reads `retryEnabled` and `retryCount` from `framework.properties`
    on each call, so you can toggle it or change the count without recompiling.

24. **What exactly re-runs on a retry?**
    The whole method lifecycle: `@BeforeMethod` (fresh driver, data, report node) → `@Test` →
    `@AfterMethod`. A retry is a clean, independent attempt, not a resume.

25. **Does a soft-assertion failure get retried too?**
    Yes — `afterInvocation` promotes a soft failure to a real `FAILURE`, and TestNG then
    consults the retry analyzer like any other failure.

**Parallel & config**

26. **How do you run tests in parallel and what did you make thread-safe?**
    `parallel="methods" thread-count="3"` in `testng.xml`. Driver, Extent node, test data, and
    the soft-assert collector are all `ThreadLocal`, because TestNG shares one class instance
    across method threads.

27. **How do test categories/groups work here?**
    `@Test(groups=…)` tags a test; `mvn test -Dgroups=smoke` filters via Surefire's
    `<groups>${groups}</groups>`. New groups need only a new annotation value.

28. **Where does browser choice come from and how is the driver binary resolved?**
    `browser` in `framework.properties`, resolved in a `switch` in `DriverManager`. Selenium
    Manager (built into Selenium 4.6+) downloads the matching driver binary automatically.

**Integrations**

29. **How are results published to qTest automatically?**
    `TestListener.onFinish` → `QTestUploader`: build JUnit XML from the in-memory results,
    POST to qTest's `auto-test-logs` (via `HttpClient`), poll the returned job, then attach the
    Extent report to the created Test Run. Gated by `qtestEnabled`; token comes from an env var.

30. **Why build the JUnit XML yourself instead of uploading Surefire's XML file?**
    Surefire writes its `target/surefire-reports/*.xml` only *after* the TestNG run hands
    control back — which is after `onFinish` runs. Reading it there would find nothing or a
    stale file, so the uploader builds the XML from the live `ITestContext` results instead.
```
