package framework.api.auth;

import framework.constants.FrameworkConstants;

/**
 * Supplies the value of the {@code ai_assist_token} session cookie used to authenticate API
 * calls. The value (a JWT) is read from the {@code AI_ASSIST_TOKEN} environment variable
 * (never committed) — copied from the browser's cookies after an SSO login.
 *
 * <p>The token is short-lived, so it is read fresh on each call; refresh it by updating the
 * environment variable.</p>
 */
public final class TokenManager {

    private TokenManager() {
        // Prevent instantiation.
    }

    /** @return the raw {@code ai_assist_token} cookie value. */
    public static String getToken() {
        String token = System.getenv(FrameworkConstants.API_TOKEN_ENV_VAR);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "API auth token missing: set the " + FrameworkConstants.API_TOKEN_ENV_VAR
                            + " environment variable to the 'ai_assist_token' cookie value.");
        }
        return token.trim();
    }
}
