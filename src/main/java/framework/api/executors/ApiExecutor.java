package framework.api.executors;

import framework.api.auth.TokenManager;
import framework.api.config.ApiConfig;
import framework.api.logging.ApiLogFilter;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * Base HTTP layer for all API clients. Builds a REST Assured request pre-configured with the
 * base URI (from {@link ApiConfig}), JSON content/accept types, and the request/response
 * logging filter ({@link ApiLogFilter}).
 *
 * <p>Three entry points let clients build positive and negative (auth) scenarios from the
 * same code path:</p>
 * <ul>
 *   <li>{@link #authed()} — valid bearer token (from {@link TokenManager})</li>
 *   <li>{@link #withToken(String)} — a specific (possibly invalid/expired) token</li>
 *   <li>{@link #noAuth()} — no Authorization header at all</li>
 * </ul>
 *
 * <p>Group clients (e.g. {@code HomeClient}) extend this and add typed endpoint methods.</p>
 */
public class ApiExecutor {

    /** Request with a valid bearer token — use for positive/edge scenarios. */
    protected RequestSpecification authed() {
        return base().header("Authorization", "Bearer " + TokenManager.getToken());
    }

    /** Request with a caller-supplied token — use for invalid/expired-token negative tests. */
    protected RequestSpecification withToken(String token) {
        return base().header("Authorization", "Bearer " + token);
    }

    /** Request with no Authorization header — use for missing-token negative tests. */
    protected RequestSpecification noAuth() {
        return base();
    }

    private RequestSpecification base() {
        return RestAssured.given()
                .baseUri(ApiConfig.baseUri())
                .filter(new ApiLogFilter())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON);
    }
}
