package framework.api.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Typed response for {@code POST /qe/report/generate}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReportGenerateResponse {

    @JsonProperty("report_url")
    private String reportUrl;

    private String status;

    public String getReportUrl() {
        return reportUrl;
    }

    public void setReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
