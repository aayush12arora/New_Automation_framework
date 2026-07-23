package framework.api.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Typed response for {@code POST /qe/test_suite/create} (201). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateSuiteResponse {

    private Data data;

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    /** Nested {@code data} object of the response. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Data {

        @JsonProperty("test_suite_id")
        private Integer testSuiteId;

        private String message;

        public Integer getTestSuiteId() {
            return testSuiteId;
        }

        public void setTestSuiteId(Integer testSuiteId) {
            this.testSuiteId = testSuiteId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
