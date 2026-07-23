package framework.api.services;

import framework.api.base.BaseService;
import framework.api.constants.ApiEndpoints;
import framework.api.models.request.AddDefectsRequest;
import framework.api.models.request.NewDefectRequest;
import framework.api.models.request.UpdateDefectRequest;
import io.restassured.response.Response;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for the "Defect Management" endpoints. Methods take the auth token as a parameter —
 * sourced by the caller from {@code customerData.get().getToken()}.
 */
public class DefectService extends BaseService {

    /** {@code POST /qe-defect/defect-management/add_defects}. */
    public Response addDefects(String token, AddDefectsRequest body) {
        setAuthToken(token);
        return postRequest(body, ApiEndpoints.ADD_DEFECTS, null);
    }

    /** {@code GET /qe-defect/defect-management/get_defects}. */
    public Response getDefects(String token, Integer testSuiteId, Integer projectId, Integer page, Integer pageSize) {
        setAuthToken(token);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("test_suite_id", testSuiteId);
        params.put("project_id", projectId);
        params.put("page", page);
        params.put("page_size", pageSize);
        return getRequest(ApiEndpoints.GET_DEFECTS, params);
    }

    /**
     * {@code GET /qe-defect/defect-workflow/get_similar_defects}. The {@code test_case_ids}
     * query param repeats (one per id).
     */
    public Response getSimilarDefects(String token, Integer testSuiteId, List<String> testCaseIds) {
        setAuthToken(token);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("test_suite_id", testSuiteId);
        params.put("test_case_ids", testCaseIds);   // REST Assured repeats the key for a list
        return getRequest(ApiEndpoints.SIMILAR_DEFECTS, params);
    }

    /** {@code GET /qe-defect/defect-workflow/get_template}. */
    public Response getTemplate(String token, Integer testSuiteId, String testCaseIds,
                                String sessionId, String projectName) {
        setAuthToken(token);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("test_suite_id", testSuiteId);
        params.put("test_case_ids", testCaseIds);
        params.put("session_id", sessionId);
        params.put("project_name", projectName);
        return getRequest(ApiEndpoints.DEFECT_TEMPLATE, params);
    }

    /** {@code POST /qe-defect/defect-management/add_new_defect} (query params + body). */
    public Response addNewDefect(String token, Integer projectId, Integer testSuiteId, NewDefectRequest body) {
        setAuthToken(token);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("project_id", projectId);
        params.put("test_suite_id", testSuiteId);
        return postRequest(body, ApiEndpoints.ADD_NEW_DEFECT, params);
    }

    /** {@code POST /qe-defect/defect-management/update_defect}. */
    public Response updateDefect(String token, UpdateDefectRequest body) {
        setAuthToken(token);
        return postRequest(body, ApiEndpoints.UPDATE_DEFECT, null);
    }
}
