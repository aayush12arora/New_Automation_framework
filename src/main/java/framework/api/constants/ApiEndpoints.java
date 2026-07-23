package framework.api.constants;

/**
 * Endpoint paths for the QE Core Services API, taken from the OpenAPI spec.
 * Kept in one place so tests/clients never hard-code URL strings.
 */
public final class ApiEndpoints {

    private ApiEndpoints() {
        // Prevent instantiation.
    }

    // Home Page
    public static final String PROJECTS = "/core-services/v1/project";
    public static final String TEST_SUITES = "/qe/project/test_suites";

    // Test Suite Management
    public static final String PROJECT_INSIGHTS = "/qe/test_suite/project_insights";
    public static final String TCM_TOOL = "/ftg/v1/functional-testcase/fetch-tcm-tool";
    public static final String ADD_TESTCASES = "/qe/test-suite/add-testcases";
    public static final String DELETE_TESTCASE = "/qe/test-suite/delete-testcase";
    public static final String CREATE_SUITE = "/qe/test_suite/create";

    // Test Execution
    public static final String RERUN_ID = "/qe/rerun_id";
    public static final String RUN_HISTORY = "/qe/test-suite/run-history";
    public static final String SUITE_DETAILS = "/qe/test-suite/details";
    public static final String REPORT_GENERATE = "/qe/report/generate";
    public static final String GITHUB_FILES = "/qe/test-case/github-files";
    public static final String RERUN = "/qe/test_suite/rerun";
    public static final String DATA_COLLECTOR_EXECUTE = "/data-collector/data-collector/execute";
    public static final String TESTCASE_DETAILS = "/qe/test-suite/testcase-details";

    // Defect Management
    public static final String ADD_DEFECTS = "/qe-defect/defect-management/add_defects";
    public static final String GET_DEFECTS = "/qe-defect/defect-management/get_defects";
    public static final String SIMILAR_DEFECTS = "/qe-defect/defect-workflow/get_similar_defects";
    public static final String DEFECT_TEMPLATE = "/qe-defect/defect-workflow/get_template";
    public static final String ADD_NEW_DEFECT = "/qe-defect/defect-management/add_new_defect";
    public static final String UPDATE_DEFECT = "/qe-defect/defect-management/update_defect";
}
