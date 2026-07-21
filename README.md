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

## Parallel execution
`testng.xml` runs with `parallel="methods" thread-count="3"` — each `@Test` method
runs on its own thread, launching its own browser instance.

The framework is built for this out of the box:
- `DriverManager` (`ThreadLocal<WebDriver>`), `ExtentReportManager`
  (`ThreadLocal<ExtentTest>`), `BaseTest.getCustomerData()` (`ThreadLocal<CustomerData>`),
  and `UIAssertions`' soft-assert collector are all thread-local, since TestNG runs a
  class's `@Test` methods on separate threads against **one shared instance** of that
  class under `parallel="methods"`. A plain instance field would be overwritten across
  threads; a `ThreadLocal` isn't.
- Tune `thread-count` to your machine's CPU/RAM — each thread opens its own browser.
  Consider `headless=true` in `framework.properties` when running many threads.
- To make an *individual* test class single-threaded relative to others, TestNG also
  supports `@Test(singleThreaded = true)` and per-class overrides if needed.

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
- `BaseTest` loads it via `JsonUtils` / `ExcelUtils` and maps it into thread-local
  storage **before** the test runs — tests call `getCustomerData()`, never parse
  files themselves.
- If no matching file exists, `getCustomerData()` returns an empty (non-null) object.
