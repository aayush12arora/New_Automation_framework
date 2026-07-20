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
```

## Structure
```
framework
├── base        BaseTest, BaseUITest, BasePage, DriverManager
├── pages       Page Objects (LoginPage)
├── api         executors/ + validators/ (REST Assured)
├── reporting   ExtentReportManager, TestListener
├── utilities   PropertyManager, WaitUtils, ScreenshotUtils, JsonUtils, StepLogger, LoggerUtil
├── data        environment / customer / database / testdata / pojo
└── constants   FrameworkConstants
```

## Key conventions
- Driver is held in a `ThreadLocal` inside `DriverManager` — no singleton.
- Page Objects extend `BasePage` and obtain the driver internally; **no** constructor injection.
- Tests extend `BaseUITest`, instantiate pages with `new LoginPage()`, and hold all assertions.
- `By` locators only — no PageFactory, no static sleeps.
