package framework.api.filters;

import framework.utilities.LoggerUtil;
import framework.utilities.StepLogger;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.slf4j.Logger;

/**
 * REST Assured filter that records every request/response as a test step — to the SLF4J log
 * <em>and</em> the Extent report (via {@link StepLogger}) — with request/response timing.
 * The auth cookie is <b>masked</b>, so its value never reaches logs or the report.
 *
 * <p>Request/response bodies render as their own JSON code block in the Extent report (via
 * {@link StepLogger#stepWithJson}), so a failed assertion has readable, formatted evidence right
 * next to it instead of a raw string dump.</p>
 */
public class LoggingFilter implements Filter {

    private static final Logger LOG = LoggerUtil.getLogger(LoggingFilter.class);

    @Override
    public Response filter(FilterableRequestSpecification requestSpec,
                           FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {

        StepLogger.stepWithJson(LOG, "API request: %s %s%s".formatted(
                requestSpec.getMethod(), requestSpec.getURI(), maskedAuth(requestSpec)),
                requestSpec.getBody());

        long start = System.currentTimeMillis();
        Response response = ctx.next(requestSpec, responseSpec);
        long tookMs = System.currentTimeMillis() - start;

        StepLogger.stepWithJson(LOG, "API response: HTTP %d in %d ms".formatted(
                response.getStatusCode(), tookMs), response.getBody().asString());
        return response;
    }

    /** Reports whether the auth cookie is present, without ever printing its value. */
    private static String maskedAuth(FilterableRequestSpecification requestSpec) {
        boolean hasAuthCookie = requestSpec.getCookies().hasCookieWithName("ai_assist_token");
        return hasAuthCookie ? " | Cookie ai_assist_token=***MASKED***" : " | (no auth cookie)";
    }
}
