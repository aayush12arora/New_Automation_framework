package framework.api.logging;

import framework.utilities.LoggerUtil;
import framework.utilities.StepLogger;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.slf4j.Logger;

/**
 * REST Assured filter that records every request/response as a test step — to the SLF4J
 * log <em>and</em> the Extent report (via {@link StepLogger}) — with request/response
 * timing. The bearer token is <b>masked</b> so it never reaches logs or the report.
 *
 * <p>Response bodies are included, so a failed assertion has the response as evidence
 * right next to it in the report.</p>
 */
public class ApiLogFilter implements Filter {

    private static final Logger LOG = LoggerUtil.getLogger(ApiLogFilter.class);
    private static final int MAX_BODY = 2000;

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                           FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {

        StepLogger.step(LOG, "API request: %s %s%s".formatted(
                requestSpec.getMethod(), requestSpec.getURI(), maskedAuth(requestSpec)));

        long start = System.currentTimeMillis();
        Response response = ctx.next(requestSpec, responseSpec);
        long tookMs = System.currentTimeMillis() - start;

        StepLogger.step(LOG, "API response: HTTP %d in %d ms | body=%s".formatted(
                response.getStatusCode(), tookMs, truncate(response.getBody().asString())));
        return response;
    }

    private static String maskedAuth(FilterableRequestSpecification requestSpec) {
        boolean hasAuth = requestSpec.getHeaders().hasHeaderWithName("Authorization");
        return hasAuth ? " | Authorization: Bearer ***MASKED***" : " | (no auth header)";
    }

    private static String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= MAX_BODY ? body : body.substring(0, MAX_BODY) + "…(truncated)";
    }
}
