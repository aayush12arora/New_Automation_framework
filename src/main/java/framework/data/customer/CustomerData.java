package framework.data.customer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * The single test-data holder for both UI and API tests.
 * Populated in {@code BaseTest} from a JSON/Excel file named after the test, so tests read
 * attributes from this object instead of parsing files themselves. Unknown attributes are
 * ignored, so one POJO backs many data files (UI fields for UI tests, API fields for API tests).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerData {

    // --- UI / login fields ---
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String email;

    // --- API auth (set by BaseApiTest, NOT loaded from a data file) ---
    private String token;

    // --- API fields (QE Core Services) ---
    private String userId;
    private Integer personaId;
    private Integer moduleId;
    private Integer projectId;
    private Integer testSuiteId;
    private String testCaseId;
    private List<String> testCaseIds;
    private String runId;
    private String sessionId;
    private String projectName;
    private Integer page;
    private Integer pageSize;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

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

    public List<String> getTestCaseIds() {
        return testCaseIds;
    }

    public void setTestCaseIds(List<String> testCaseIds) {
        this.testCaseIds = testCaseIds;
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
