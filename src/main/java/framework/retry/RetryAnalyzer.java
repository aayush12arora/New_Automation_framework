package framework.retry;

import framework.constants.FrameworkConstants;
import framework.utilities.LoggerUtil;
import framework.utilities.PropertyManager;
import org.slf4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Retries a failed {@code @Test} method, controlled entirely by
 * {@code framework.properties}: {@code retryEnabled} (on/off) and
 * {@code retryCount} (max retries). TestNG creates a fresh instance of this
 * class per test method, so the attempt counter needs no synchronisation.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger LOG = LoggerUtil.getLogger(RetryAnalyzer.class);

    private int attempts = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (!PropertyManager.getBoolean(FrameworkConstants.RETRY_ENABLED)) {
            return false;
        }
        int maxRetries = PropertyManager.getInt(FrameworkConstants.RETRY_COUNT);
        if (attempts >= maxRetries) {
            return false;
        }
        attempts++;
        LOG.warn("Retrying '{}' - attempt {} of {}", result.getName(), attempts, maxRetries);
        return true;
    }
}
