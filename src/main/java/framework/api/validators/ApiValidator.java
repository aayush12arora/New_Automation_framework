package framework.api.validators;

import framework.utilities.LoggerUtil;
import io.restassured.response.Response;
import org.slf4j.Logger;

/**
 * Minimal validator for API responses.
 * Provides small, reusable checks that tests can compose.
 */
public class ApiValidator {

    private static final Logger LOG = LoggerUtil.getLogger(ApiValidator.class);

    public boolean hasStatus(Response response, int expectedStatus) {
        int actual = response.getStatusCode();
        LOG.info("Validate status: expected={}, actual={}", expectedStatus, actual);
        return actual == expectedStatus;
    }

    public boolean bodyContains(Response response, String expectedText) {
        boolean contains = response.getBody().asString().contains(expectedText);
        LOG.info("Validate body contains '{}': {}", expectedText, contains);
        return contains;
    }
}
