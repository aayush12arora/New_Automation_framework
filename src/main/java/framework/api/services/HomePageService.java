package framework.api.services;

import framework.api.auth.TokenManager;
import framework.api.base.BaseService;
import framework.api.constants.ApiEndpoints;
import io.restassured.response.Response;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service for the "Home Page" endpoints (mirrors the reference framework's service classes,
 * e.g. {@code AuthService}). Endpoint methods return a {@link Response} the tests deserialize
 * and assert on.
 */
public class HomePageService extends BaseService {

    /** {@code GET /qe/project/test_suites} with a valid token — positive/edge scenarios. */
    public Response getTestSuites(String status, Integer page, Integer pageSize,
                                  String sortBy, String sortOrder, Integer projectId) {
        setAuthToken(TokenManager.getToken());
        return getRequest(ApiEndpoints.TEST_SUITES,
                testSuiteParams(status, page, pageSize, sortBy, sortOrder, projectId));
    }

    /** Same endpoint with no auth header — for the missing-token negative scenario. */
    public Response getTestSuitesWithoutAuth(String status, Integer page, Integer pageSize,
                                             String sortBy, String sortOrder, Integer projectId) {
        return getRequest(ApiEndpoints.TEST_SUITES,
                testSuiteParams(status, page, pageSize, sortBy, sortOrder, projectId));
    }

    /** Builds the query map, including only the non-null params (so a test can omit one). */
    private Map<String, Object> testSuiteParams(String status, Integer page, Integer pageSize,
                                                String sortBy, String sortOrder, Integer projectId) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (status != null) {
            params.put("status", status);
        }
        if (page != null) {
            params.put("page", page);
        }
        if (pageSize != null) {
            params.put("page_size", pageSize);
        }
        if (sortBy != null) {
            params.put("sort_by", sortBy);
        }
        if (sortOrder != null) {
            params.put("sort_order", sortOrder);
        }
        if (projectId != null) {
            params.put("project_id", projectId);
        }
        return params;
    }
}
