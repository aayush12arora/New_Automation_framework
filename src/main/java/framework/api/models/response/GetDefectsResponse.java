package framework.api.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Typed response for {@code GET /qe-defect/defect-management/get_defects}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetDefectsResponse {

    private List<Object> defects;
    private Integer total;
    private Integer page;

    public List<Object> getDefects() {
        return defects;
    }

    public void setDefects(List<Object> defects) {
        this.defects = defects;
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
}
