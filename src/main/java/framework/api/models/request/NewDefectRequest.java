package framework.api.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request body for {@code POST /qe-defect/defect-management/add_new_defect}.
 * Built with the fluent {@link Builder}.
 */
public class NewDefectRequest {

    @JsonProperty("issue_type")
    private final String issueType;

    @JsonProperty("title")
    private final String title;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("severity")
    private final String severity;

    @JsonProperty("priority")
    private final String priority;

    @JsonProperty("account_id")
    private final String accountId;

    @JsonProperty("project_key")
    private final String projectKey;

    @JsonProperty("test_case_id")
    private final String testCaseId;

    @JsonProperty("labels")
    private final List<String> labels;

    private NewDefectRequest(Builder builder) {
        this.issueType = builder.issueType;
        this.title = builder.title;
        this.description = builder.description;
        this.severity = builder.severity;
        this.priority = builder.priority;
        this.accountId = builder.accountId;
        this.projectKey = builder.projectKey;
        this.testCaseId = builder.testCaseId;
        this.labels = builder.labels;
    }

    public static class Builder {
        private String issueType;
        private String title;
        private String description;
        private String severity;
        private String priority;
        private String accountId;
        private String projectKey;
        private String testCaseId;
        private List<String> labels;

        public Builder issueType(String v) {
            this.issueType = v;
            return this;
        }

        public Builder title(String v) {
            this.title = v;
            return this;
        }

        public Builder description(String v) {
            this.description = v;
            return this;
        }

        public Builder severity(String v) {
            this.severity = v;
            return this;
        }

        public Builder priority(String v) {
            this.priority = v;
            return this;
        }

        public Builder accountId(String v) {
            this.accountId = v;
            return this;
        }

        public Builder projectKey(String v) {
            this.projectKey = v;
            return this;
        }

        public Builder testCaseId(String v) {
            this.testCaseId = v;
            return this;
        }

        public Builder labels(List<String> v) {
            this.labels = v;
            return this;
        }

        public NewDefectRequest build() {
            return new NewDefectRequest(this);
        }
    }
}
