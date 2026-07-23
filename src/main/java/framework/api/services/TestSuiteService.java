package framework.api.services;

import framework.api.base.BaseService;
import framework.api.constants.ApiEndpoints;
import framework.api.models.request.AddTestcasesRequest;
import framework.api.models.request.CreateSuiteRequest;
import io.restassured.response.Response;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service for the "Test Suite Management" endpoints. Methods take the auth token as a
 * parameter — sourced by the caller from {@code customerData.get().getToken()}.
 */
public class TestSuiteService extends BaseService {

    /** {@code GET /qe/test_suite/project_insights}. */
    public Response getProjectInsights(String token, Integer projectId) {
        setAuthToken(token);
        return getRequest(ApiEndpoints.PROJECT_INSIGHTS, singleParam("project_id", projectId));
    }

    /** {@code GET /ftg/v1/functional-testcase/fetch-tcm-tool}. */
    public Response getTcmTool(String token, Integer projectId) {
        setAuthToken(token);
        return getRequest(ApiEndpoints.TCM_TOOL, singleParam("project_id", projectId));
    }

    /** {@code POST /qe/test-suite/add-testcases}. */
    public Response addTestcases(String token, AddTestcasesRequest body) {
        setAuthToken(token);
        return postRequest(body, ApiEndpoints.ADD_TESTCASES, null);
    }

    /** {@code DELETE /qe/test-suite/delete-testcase}. */
    public Response deleteTestcase(String token, Integer projectId, Integer testSuiteId, String testCaseIds) {
        setAuthToken(token);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("project_id", projectId);
        params.put("test_suite_id", testSuiteId);
        params.put("test_case_ids", testCaseIds);
        return deleteRequest(ApiEndpoints.DELETE_TESTCASE, params);
    }

    /** {@code POST /qe/test_suite/create}. */
    public Response createSuite(String token, CreateSuiteRequest body) {
        setAuthToken(token);
        return postRequest(body, ApiEndpoints.CREATE_SUITE, null);
    }

    private Map<String, Object> singleParam(String key, Object value) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put(key, value);
        return params;
    }
}
