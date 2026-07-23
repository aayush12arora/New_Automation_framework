package framework.api.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Typed response for {@code GET /qe/project/test_suites/v2}.
 *
 * <p><b>Best-effort:</b> this endpoint has no documented schema (it wasn't in the OpenAPI spec,
 * only in the working Postman collection). The shape mirrors {@link TestSuitesResponse} as the
 * most reasonable assumption for a versioned variant of the same resource; adjust the fields
 * once a real response is seen.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestSuitesV2Response {

    @JsonProperty("test_suites")
    private List<Object> testSuites;

    private Integer total;

    public List<Object> getTestSuites() {
        return testSuites;
    }

    public void setTestSuites(List<Object> testSuites) {
        this.testSuites = testSuites;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }
}
