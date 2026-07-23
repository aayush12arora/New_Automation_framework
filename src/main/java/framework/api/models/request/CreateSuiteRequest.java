package framework.api.models.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for {@code POST /qe/test_suite/create}. Built with the fluent {@link Builder}
 * (mirrors the reference framework's {@code ProfileRequest.Builder}).
 */
public class CreateSuiteRequest {

    @JsonProperty("test_suite_name")
    private final String testSuiteName;

    @JsonProperty("created_by")
    private final String createdBy;

    @JsonProperty("updated_by")
    private final String updatedBy;

    @JsonProperty("project_id")
    private final String projectId;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("tool")
    private final String tool;

    @JsonProperty("browser")
    private final String browser;

    @JsonProperty("language")
    private final String language;

    @JsonProperty("scheduled_test_suite")
    private final String scheduledTestSuite;

    private CreateSuiteRequest(Builder builder) {
        this.testSuiteName = builder.testSuiteName;
        this.createdBy = builder.createdBy;
        this.updatedBy = builder.updatedBy;
        this.projectId = builder.projectId;
        this.description = builder.description;
        this.tool = builder.tool;
        this.browser = builder.browser;
        this.language = builder.language;
        this.scheduledTestSuite = builder.scheduledTestSuite;
    }

    public static class Builder {
        private String testSuiteName;
        private String createdBy;
        private String updatedBy;
        private String projectId;
        private String description;
        private String tool;
        private String browser;
        private String language;
        private String scheduledTestSuite;

        public Builder testSuiteName(String v) {
            this.testSuiteName = v;
            return this;
        }

        public Builder createdBy(String v) {
            this.createdBy = v;
            return this;
        }

        public Builder updatedBy(String v) {
            this.updatedBy = v;
            return this;
        }

        public Builder projectId(String v) {
            this.projectId = v;
            return this;
        }

        public Builder description(String v) {
            this.description = v;
            return this;
        }

        public Builder tool(String v) {
            this.tool = v;
            return this;
        }

        public Builder browser(String v) {
            this.browser = v;
            return this;
        }

        public Builder language(String v) {
            this.language = v;
            return this;
        }

        public Builder scheduledTestSuite(String v) {
            this.scheduledTestSuite = v;
            return this;
        }

        public CreateSuiteRequest build() {
            return new CreateSuiteRequest(this);
        }
    }
}
