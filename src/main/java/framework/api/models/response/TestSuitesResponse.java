package framework.api.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Typed response model for {@code GET /qe/project/test_suites}, deserialized from the response
 * via {@code response.as(TestSuitesResponse.class)} so tests assert on getters rather than raw
 * JSON paths (mirrors the reference framework's {@code models/response} POJOs).
 *
 * <p>{@code @JsonProperty} maps the API's snake_case keys onto camelCase fields; unknown keys
 * are ignored so the model tolerates extra fields.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestSuitesResponse {

    @JsonProperty("test_suites")
    private List<Object> testSuites;

    private Integer total;
    private Integer page;

    @JsonProperty("page_size")
    private Integer pageSize;

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
