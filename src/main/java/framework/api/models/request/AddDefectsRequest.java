package framework.api.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Request body for {@code POST /qe-defect/defect-management/add_defects}. */
public class AddDefectsRequest {

    @JsonProperty("test_case_ids")
    private final List<String> testCaseIds;

    @JsonProperty("user_id")
    private final String userId;

    @JsonProperty("test_suite_id")
    private final Integer testSuiteId;

    public AddDefectsRequest(List<String> testCaseIds, String userId, Integer testSuiteId) {
        this.testCaseIds = testCaseIds;
        this.userId = userId;
        this.testSuiteId = testSuiteId;
    }

    public List<String> getTestCaseIds() {
        return testCaseIds;
    }

    public String getUserId() {
        return userId;
    }

    public Integer getTestSuiteId() {
        return testSuiteId;
    }
}
