package framework.api.client;

import framework.api.constants.ApiEndpoints;
import framework.api.executors.ApiExecutor;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * Client for the "Home Page" endpoints.
 * Typed methods build the request; tests assert on the returned {@link Response}.
 */
public class HomeClient extends ApiExecutor {

    /** {@code GET /qe/project/test_suites} with a valid token (positive/edge scenarios). */
    public Response getTestSuites(String status, Integer page, Integer pageSize,
                                  String sortBy, String sortOrder, Integer projectId) {
        return callTestSuites(authed(), status, page, pageSize, sortBy, sortOrder, projectId);
    }

    /** Same endpoint with no auth header — for the missing-token negative scenario. */
    public Response getTestSuitesNoAuth(String status, Integer page, Integer pageSize,
                                        String sortBy, String sortOrder, Integer projectId) {
        return callTestSuites(noAuth(), status, page, pageSize, sortBy, sortOrder, projectId);
    }

    /**
     * Adds only the non-null query params, so a test can omit a required one (e.g. pass
     * {@code null} for {@code status}) to exercise a validation-error path.
     */
    private Response callTestSuites(RequestSpecification request, String status, Integer page,
                                    Integer pageSize, String sortBy, String sortOrder, Integer projectId) {
        if (status != null) {
            request.queryParam("status", status);
        }
        if (page != null) {
            request.queryParam("page", page);
        }
        if (pageSize != null) {
            request.queryParam("page_size", pageSize);
        }
        if (sortBy != null) {
            request.queryParam("sort_by", sortBy);
        }
        if (sortOrder != null) {
            request.queryParam("sort_order", sortOrder);
        }
        if (projectId != null) {
            request.queryParam("project_id", projectId);
        }
        return request.get(ApiEndpoints.TEST_SUITES);
    }
}
