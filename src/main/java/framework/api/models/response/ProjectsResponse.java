package framework.api.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Typed response for {@code GET /core-services/v1/project}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectsResponse {

    private List<Object> projects;

    public List<Object> getProjects() {
        return projects;
    }

    public void setProjects(List<Object> projects) {
        this.projects = projects;
    }
}
