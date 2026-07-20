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

## Reporting
- **Extent report:** `target/extent-report/index.html` — every step is logged with a
  screenshot, and a final pass/fail screenshot is attached at the end of each test.
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
├── utilities   PropertyManager, WaitUtils, ScreenshotUtils, JsonUtils, StepLogger, LoggerUtil
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

### Assertions
- Tests **never** call raw `Assert.*`. All UI assertions go through `UIAssertions`,
  exposed as `assertions` on `BaseUITest`.
- Both **hard** (`assertTrue`, `assertEquals`, …) and **soft**
  (`softAssertTrue`, `softAssertEquals`, …) assertions are supported.
- Soft assertions are collected per thread and **finalised automatically** by
  `TestListener` after each test — tests do not call `assertAll()`.
- Database validations live in their own class, `data.database.DatabaseValidator`.

### Test data
- Each test's JSON is named after the test method: `testdata/<testMethodName>.json`.
- `BaseTest` reads it via `JsonUtils` and maps it into `customerData` (a `CustomerData`
  object) **before** the test runs — tests read `customerData`, never parse JSON.
- If no matching JSON exists, `customerData` is an empty (non-null) object.
