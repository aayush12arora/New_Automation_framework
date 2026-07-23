package framework.api.config;

import framework.constants.FrameworkConstants;
import framework.utilities.PropertyManager;

/**
 * Resolves API runtime configuration from {@code framework.properties} (overridable with
 * {@code -D} system properties, e.g. {@code -Dapi.mode=live}).
 *
 * <p>Two modes:</p>
 * <ul>
 *   <li><b>mock</b> — requests go to a local WireMock server ({@code http://localhost:<port>}),
 *       so the suite runs anywhere with no live environment or real token.</li>
 *   <li><b>live</b> — requests go to the real base URL and require a real bearer token.</li>
 * </ul>
 */
public final class ApiConfig {

    private ApiConfig() {
        // Prevent instantiation.
    }

    public static boolean isMock() {
        return FrameworkConstants.API_MODE_MOCK.equalsIgnoreCase(
                PropertyManager.get(FrameworkConstants.API_MODE));
    }

    public static int mockPort() {
        return PropertyManager.getInt(FrameworkConstants.API_MOCK_PORT);
    }

    /** @return the base URI requests are sent to, depending on the mode. */
    public static String baseUri() {
        return isMock()
                ? "http://localhost:" + mockPort()
                : PropertyManager.get(FrameworkConstants.API_BASE_URL);
    }
}
