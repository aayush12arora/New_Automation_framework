package framework.api.auth;

import framework.api.config.ApiConfig;
import framework.constants.FrameworkConstants;

/**
 * Supplies the HTTP Bearer token for secured API calls.
 *
 * <ul>
 *   <li><b>mock</b> mode — returns a dummy token, so no real credential is needed.</li>
 *   <li><b>live</b> mode — reads the token from the {@code QE_API_TOKEN} environment
 *       variable (never committed). The token is short-lived (~30 min), so it is read
 *       fresh on each call; refresh it by updating the environment variable.</li>
 * </ul>
 *
 * <p>A leading {@code "Bearer "} in the supplied value is stripped, since callers add the
 * scheme themselves — this tolerates the token being pasted either way.</p>
 */
public final class TokenManager {

    private static final String MOCK_TOKEN = "mock-token";

    private TokenManager() {
        // Prevent instantiation.
    }

    /** @return the raw bearer token (without the {@code "Bearer "} prefix). */
    public static String getToken() {
        if (ApiConfig.isMock()) {
            return MOCK_TOKEN;
        }
        String token = System.getenv(FrameworkConstants.API_TOKEN_ENV_VAR);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "API bearer token missing: set the " + FrameworkConstants.API_TOKEN_ENV_VAR
                            + " environment variable (or run with api.mode=mock).");
        }
        String trimmed = token.trim();
        return trimmed.regionMatches(true, 0, "Bearer ", 0, 7) ? trimmed.substring(7).trim() : trimmed;
    }
}
