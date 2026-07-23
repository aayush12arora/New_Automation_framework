package framework.api.services;

import framework.api.auth.TokenManager;
import framework.api.base.BaseService;
import framework.api.constants.ApiEndpoints;
import framework.api.models.request.DataCollectorRequest;
import framework.api.models.request.ReportGenerateRequest;
import framework.api.models.request.RerunRequest;
import io.restassured.response.Response;

import java.util.LinkedHashMap;
import java.util.Map;

/** Service for the "Test Execution" endpoints. */
public class TestExecutionService extends BaseService {

    /** {@code GET /qe/rerun_id}. */
    public Response getRunId(Integer projectId, Integer testSuiteId) {
        setAuthToken(TokenManager.getToken());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("project_id", projectId);
        params.put("test_suite_id", testSuiteId);
        return getRequest(ApiEndpoints.RERUN_ID, params);
    }

    /** {@code GET /qe/test-suite/run-history}. */
    public Response getRunHistory(String userId, Integer projectId, Integer testSuiteId,
                                  Integer page, Integer pageSize) {
        setAuthToken(TokenManager.getToken());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("user_id", userId);
        params.put("project_id", projectId);
        params.put("test_suite_id", testSuiteId);
        params.put("page", page);
        params.put("page_size", pageSize);
        return getRequest(ApiEndpoints.RUN_HISTORY, params);
    }

    /** {@code GET /qe/test-suite/details}. */
    public Response getSuiteDetails(Integer projectId, String userId, Integer testSuiteId,
                                    Integer pageSize, Integer page, String runId) {
        setAuthToken(TokenManager.getToken());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("project_id", projectId);
        params.put("user_id", userId);
        params.put("test_suite_id", testSuiteId);
        params.put("page_size", pageSize);
        params.put("page", page);
        params.put("run_id", runId);
        return getRequest(ApiEndpoints.SUITE_DETAILS, params);
    }

    /** {@code POST /qe/report/generate}. */
    public Response generateReport(ReportGenerateRequest body) {
        setAuthToken(TokenManager.getToken());
        return postRequest(body, ApiEndpoints.REPORT_GENERATE, null);
    }

    /** {@code GET /qe/test-case/github-files}. */
    public Response getGithubFiles(Integer projectId, Integer testSuiteId, String testCaseId) {
        setAuthToken(TokenManager.getToken());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("project_id", projectId);
        params.put("test_suite_id", testSuiteId);
        params.put("test_case_id", testCaseId);
        return getRequest(ApiEndpoints.GITHUB_FILES, params);
    }

    /** {@code POST /qe/test_suite/rerun}. */
    public Response rerun(RerunRequest body) {
        setAuthToken(TokenManager.getToken());
        return postRequest(body, ApiEndpoints.RERUN, null);
    }

    /** {@code POST /data-collector/data-collector/execute} (query params + body). */
    public Response dataCollectorExecute(Integer projectId, Integer testSuiteId, String runId,
                                         DataCollectorRequest body) {
        setAuthToken(TokenManager.getToken());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("project_id", projectId);
        params.put("test_suite_id", testSuiteId);
        params.put("run_id", runId);
        return postRequest(body, ApiEndpoints.DATA_COLLECTOR_EXECUTE, params);
    }

    /** {@code GET /qe/test-suite/testcase-details}. */
    public Response getTestcaseDetails(Integer projectId, String testCaseId, Integer testSuiteId) {
        setAuthToken(TokenManager.getToken());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("project_id", projectId);
        params.put("test_case_id", testCaseId);
        params.put("test_suite_id", testSuiteId);
        return getRequest(ApiEndpoints.TESTCASE_DETAILS, params);
    }
}
