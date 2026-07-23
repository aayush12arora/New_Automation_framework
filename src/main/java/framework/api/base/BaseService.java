package framework.api.base;

import framework.api.config.ApiConfig;
import framework.api.filters.LoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * RestAssured wrapper and base for all API service classes (mirrors the reference
 * framework's {@code BaseService}). Domain services (e.g. {@code HomePageService}) extend this
 * and expose endpoint methods that return a {@link Response}.
 *
 * <p>Follows the reference conventions — {@code setAuthToken} + {@code getRequest}/
 * {@code postRequest}/{@code putRequest}/{@code deleteRequest} — with two framework upgrades:
 * a fresh {@link RequestSpecification} is built per call (so a reused service instance can't
 * leak query params between calls), and the token is applied from a stored value so the same
 * service can make both authed and no-auth (negative) calls. The base URI comes from
 * {@link ApiConfig} and every request carries the {@link LoggingFilter}.</p>
 */
public class BaseService {

    private String authToken;

    /** Sets the bearer token used by subsequent requests. Not calling it = no auth header. */
    protected void setAuthToken(String token) {
        this.authToken = token;
    }

    protected Response getRequest(String endpoint, Map<String, Object> queryParams) {
        return withQuery(baseSpec(), queryParams).get(endpoint);
    }

    protected Response postRequest(Object body, String endpoint, Map<String, Object> queryParams) {
        RequestSpecification spec = withQuery(baseSpec().contentType(ContentType.JSON), queryParams);
        if (body != null) {
            spec.body(body);
        }
        return spec.post(endpoint);
    }

    protected Response putRequest(Object body, String endpoint, Map<String, Object> queryParams) {
        RequestSpecification spec = withQuery(baseSpec().contentType(ContentType.JSON), queryParams);
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
                .accept(ContentType.JSON);
        if (authToken != null && !authToken.isBlank()) {
            spec.header("Authorization", "Bearer " + authToken);
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
