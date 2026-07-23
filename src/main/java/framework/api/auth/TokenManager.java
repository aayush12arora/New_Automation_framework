package framework.api.auth;

import framework.constants.FrameworkConstants;

/**
 * Supplies the HTTP Bearer token for secured API calls, read from the {@code QE_API_TOKEN}
 * environment variable (never committed). The token is short-lived (~30 min), so it is read
 * fresh on each call — refresh it by updating the environment variable.
 *
 * <p>A leading {@code "Bearer "} in the value is stripped, since callers add the scheme
 * themselves, so the token can be pasted either way.</p>
 */
public final class TokenManager {

    private TokenManager() {
        // Prevent instantiation.
    }

    /** @return the raw bearer token (without the {@code "Bearer "} prefix). */
    public static String getToken() {
        String token = System.getenv(FrameworkConstants.API_TOKEN_ENV_VAR);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "API bearer token missing: set the " + FrameworkConstants.API_TOKEN_ENV_VAR
                            + " environment variable.");
        }
        String trimmed = token.trim();
        return trimmed.regionMatches(true, 0, "Bearer ", 0, 7) ? trimmed.substring(7).trim() : trimmed;
    }
}
