package framework.api.base;

import framework.api.config.ApiConfig;
import framework.api.filters.LoggingFilter;
import framework.constants.FrameworkConstants;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * RestAssured wrapper and base for all API service classes (mirrors the reference framework's
 * {@code BaseService}). Domain services (e.g. {@code HomePageService}) extend this and expose
 * endpoint methods that return a {@link Response}.
 *
 * <p><b>Auth is a cookie, not a header.</b> The system authenticates via the
 * {@code ai_assist_token} session cookie (a JWT), so {@link #setAuthToken(String)} stores the
 * value and every request carries it as a cookie. A fresh {@link RequestSpecification} is built
 * per call (so a reused service can't leak query params), and the token is applied from the
 * stored value — so the same service can make both authed and no-auth (negative) calls.</p>
 */
public class BaseService {

    private String authToken;

    /** Sets the {@code ai_assist_token} cookie value used by subsequent requests. */
    protected void setAuthToken(String token) {
        this.authToken = token;
    }

    protected Response getRequest(String endpoint, Map<String, Object> queryParams) {
        return withQuery(baseSpec(), queryParams).get(endpoint);
    }

    protected Response postRequest(Object body, String endpoint, Map<String, Object> queryParams) {
        RequestSpecification spec = withQuery(baseSpec(), queryParams);
        if (body != null) {
            spec.body(body);
        }
        return spec.post(endpoint);
    }

    protected Response putRequest(Object body, String endpoint, Map<String, Object> queryParams) {
        RequestSpecification spec = withQuery(baseSpec(), queryParams);
        if (body != null) {
            spec.body(body);
        }
        return spec.put(endpoint);
    }

    protected Response deleteRequest(String endpoint, Map<String, Object> queryParams) {
        return withQuery(baseSpec(), queryParams).delete(endpoint);
    }

    private RequestSpecification baseSpec() {
        RequestSpecification spec = given()
                .baseUri(ApiConfig.baseUri())
                .filter(new LoggingFilter())
                .accept("*/*")
                .contentType(ContentType.JSON);
        if (authToken != null && !authToken.isBlank()) {
            spec.cookie(FrameworkConstants.API_AUTH_COOKIE, authToken);
        }
        return spec;
    }

    private RequestSpecification withQuery(RequestSpecification spec, Map<String, Object> queryParams) {
        if (queryParams != null && !queryParams.isEmpty()) {
            spec.queryParams(queryParams);
        }
        return spec;
    }
}
