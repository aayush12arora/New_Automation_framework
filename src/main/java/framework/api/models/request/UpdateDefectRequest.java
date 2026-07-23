package framework.api.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Request body for {@code POST /qe-defect/defect-management/update_defect}. */
public class UpdateDefectRequest {

    @JsonProperty("defect_data")
    private final List<DefectData> defectData;

    @JsonProperty("test_case_id")
    private final String testCaseId;

    @JsonProperty("test_suite_id")
    private final Integer testSuiteId;

    public UpdateDefectRequest(List<DefectData> defectData, String testCaseId, Integer testSuiteId) {
        this.defectData = defectData;
        this.testCaseId = testCaseId;
        this.testSuiteId = testSuiteId;
    }

    public List<DefectData> getDefectData() {
        return defectData;
    }

    public String getTestCaseId() {
        return testCaseId;
    }

    public Integer getTestSuiteId() {
        return testSuiteId;
    }

    /** One entry in the {@code defect_data} array. */
    public static class DefectData {

        @JsonProperty("defect_id")
        private final String defectId;

        @JsonProperty("defect_link")
        private final String defectLink;

        public DefectData(String defectId, String defectLink) {
            this.defectId = defectId;
            this.defectLink = defectLink;
        }

        public String getDefectId() {
            return defectId;
        }

        public String getDefectLink() {
            return defectLink;
        }
    }
}
