package framework.data.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Shared API test data, loaded from {@code testdata/apiTestData.json} (or {@code .xlsx})
 * by {@code BaseApiTest} — so endpoint IDs and query values are never hard-coded in tests.
 * Unknown attributes are ignored so one file can serve many endpoints.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiTestData {

    private String userId;
    private Integer personaId;
    private Integer moduleId;
    private Integer projectId;
    private Integer testSuiteId;
    private String testCaseId;
    private String runId;
    private String sessionId;
    private String projectName;
    private Integer page;
    private Integer pageSize;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getPersonaId() {
        return personaId;
    }

    public void setPersonaId(Integer personaId) {
        this.personaId = personaId;
    }

    public Integer getModuleId() {
        return moduleId;
    }

    public void setModuleId(Integer moduleId) {
        this.moduleId = moduleId;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public Integer getTestSuiteId() {
        return testSuiteId;
    }

    public void setTestSuiteId(Integer testSuiteId) {
        this.testSuiteId = testSuiteId;
    }

    public String getTestCaseId() {
        return testCaseId;
    }

    public void setTestCaseId(String testCaseId) {
        this.testCaseId = testCaseId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
