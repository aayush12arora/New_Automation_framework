package framework.api.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/** Typed response for {@code GET /qe/test-suite/testcase-details}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestcaseDetailsResponse {

    @JsonProperty("test_case")
    private TestCase testCase;

    public TestCase getTestCase() {
        return testCase;
    }

    public void setTestCase(TestCase testCase) {
        this.testCase = testCase;
    }

    /** Nested {@code test_case} object of the response. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TestCase {

        private String id;
        private String name;
        private String status;
        private Map<String, Object> details;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Map<String, Object> getDetails() {
            return details;
        }

        public void setDetails(Map<String, Object> details) {
            this.details = details;
        }
    }
}
