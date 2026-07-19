package framework.base;

import framework.constants.FrameworkConstants;
import framework.utilities.LoggerUtil;
import framework.utilities.PropertyManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;

import java.time.Duration;

/**
 * Owns the {@link WebDriver} instance for the current thread.
 * Uses {@link ThreadLocal} so parallel tests never share a driver.
 * No singleton — the driver lifecycle is bound to the running thread.
 */
public final class DriverManager {

    private static final Logger LOG = LoggerUtil.getLogger(DriverManager.class);
    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverManager() {
        // Prevent instantiation.
    }

    /** Creates a driver for the configured browser and binds it to the current thread. */
    public static void setDriver() {
        String browser = PropertyManager.get(FrameworkConstants.BROWSER);
        boolean headless = PropertyManager.getBoolean(FrameworkConstants.HEADLESS);
        int implicitWait = PropertyManager.getInt(FrameworkConstants.IMPLICIT_WAIT);

        WebDriver driver = createDriver(browser, headless);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(implicitWait));
        driver.manage().window().maximize();

        DRIVER.set(driver);
        LOG.info("Driver created for browser '{}' (headless={})", browser, headless);
    }

    /** @return the driver bound to the current thread. */
    public static WebDriver getDriver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException("Driver has not been initialised. Call setDriver() first.");
        }
        return driver;
    }

    /** Quits the driver and clears it from the current thread. */
    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            driver.quit();
            DRIVER.remove();
            LOG.info("Driver quit and removed from thread");
        }
    }

    private static WebDriver createDriver(String browser, boolean headless) {
        return switch (browser.toLowerCase()) {
            case "firefox" -> {
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions options = new FirefoxOptions();
                if (headless) {
                    options.addArguments("-headless");
                }
                LOG.info("Launching Firefox");
                yield new FirefoxDriver(options);
            }
            case "chrome" -> {
                WebDriverManager.chromedriver().setup();
                ChromeOptions options = new ChromeOptions();
                if (headless) {
                    options.addArguments("--headless=new");
                }
                LOG.info("Launching Chrome");
                yield new ChromeDriver(options);
            }
            case "edge" -> {
                WebDriverManager.edgedriver().setup();
                EdgeOptions options = new EdgeOptions();
                if (headless) {
                    options.addArguments("--headless=new");
                }
                LOG.info("Launching Edge");
                yield new EdgeDriver(options);
            }
            default -> throw new IllegalArgumentException("Unsupported browser: " + browser);
        };
    }
}
