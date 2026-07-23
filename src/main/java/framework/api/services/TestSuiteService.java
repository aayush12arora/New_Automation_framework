package framework.api.services;

import framework.api.auth.TokenManager;
import framework.api.base.BaseService;
import framework.api.constants.ApiEndpoints;
import framework.api.models.request.AddTestcasesRequest;
import framework.api.models.request.CreateSuiteRequest;
import io.restassured.response.Response;

import java.util.LinkedHashMap;
import java.util.Map;

/** Service for the "Test Suite Management" endpoints. */
public class TestSuiteService extends BaseService {

    /** {@code GET /qe/test_suite/project_insights}. */
    public Response getProjectInsights(Integer projectId) {
        setAuthToken(TokenManager.getToken());
        return getRequest(ApiEndpoints.PROJECT_INSIGHTS, singleParam("project_id", projectId));
    }

    /** {@code GET /ftg/v1/functional-testcase/fetch-tcm-tool}. */
    public Response getTcmTool(Integer projectId) {
        setAuthToken(TokenManager.getToken());
        return getRequest(ApiEndpoints.TCM_TOOL, singleParam("project_id", projectId));
    }

    /** {@code POST /qe/test-suite/add-testcases}. */
    public Response addTestcases(AddTestcasesRequest body) {
        setAuthToken(TokenManager.getToken());
        return postRequest(body, ApiEndpoints.ADD_TESTCASES, null);
    }

    /** {@code DELETE /qe/test-suite/delete-testcase}. */
    public Response deleteTestcase(Integer projectId, Integer testSuiteId, String testCaseIds) {
        setAuthToken(TokenManager.getToken());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("project_id", projectId);
        params.put("test_suite_id", testSuiteId);
        params.put("test_case_ids", testCaseIds);
        return deleteRequest(ApiEndpoints.DELETE_TESTCASE, params);
    }

    /** {@code POST /qe/test_suite/create}. */
    public Response createSuite(CreateSuiteRequest body) {
        setAuthToken(TokenManager.getToken());
        return postRequest(body, ApiEndpoints.CREATE_SUITE, null);
    }

    private Map<String, Object> singleParam(String key, Object value) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put(key, value);
        return params;
    }
}
