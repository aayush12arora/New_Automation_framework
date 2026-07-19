# Selenium Automation Framework (Java 21)

Minimal, modular Selenium 4 + TestNG framework. Firefox is the default browser.

## Requirements
- Java 21
- Maven 3.9+
- Firefox installed (drivers are resolved automatically by WebDriverManager)

## Run
```bash
mvn clean test
```

## Configuration
Edit `src/main/resources/framework.properties`:
```properties
browser=firefox      # firefox | chrome | edge
headless=false
url=https://example.com
implicitWait=10
explicitWait=20
```

## Structure
```
framework
├── base        BaseTest, BaseUITest, BasePage, DriverManager
├── pages       Page Objects (LoginPage)
├── api         executors/ + validators/ (REST Assured)
├── utilities   PropertyManager, WaitUtils, ScreenshotUtils, JsonUtils, LoggerUtil
├── data        environment / customer / database / testdata / pojo
└── constants   FrameworkConstants
```

## Key conventions
- Driver is held in a `ThreadLocal` inside `DriverManager` — no singleton.
- Page Objects extend `BasePage` and obtain the driver internally; **no** constructor injection.
- Tests extend `BaseUITest`, instantiate pages with `new LoginPage()`, and hold all assertions.
- `By` locators only — no PageFactory, no static sleeps.
