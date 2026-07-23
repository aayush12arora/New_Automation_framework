package framework.api.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Typed response for {@code GET /qe/test-suite/run-history}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RunHistoryResponse {

    private List<Object> history;
    private Integer total;

    public List<Object> getHistory() {
        return history;
    }

    public void setHistory(List<Object> history) {
        this.history = history;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }
}
