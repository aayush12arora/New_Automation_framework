package framework.api.config;

import framework.constants.FrameworkConstants;
import framework.utilities.PropertyManager;

/**
 * API runtime configuration, read from {@code framework.properties}
 * (overridable with {@code -Dapi.baseUrl=...}).
 */
public final class ApiConfig {

    private ApiConfig() {
        // Prevent instantiation.
    }

    /** @return the base URI every API request is sent to. */
    public static String baseUri() {
        return PropertyManager.get(FrameworkConstants.API_BASE_URL);
    }
}
