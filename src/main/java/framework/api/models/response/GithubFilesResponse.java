package framework.api.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Typed response for {@code GET /qe/test-case/github-files}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubFilesResponse {

    private List<FileEntry> files;

    public List<FileEntry> getFiles() {
        return files;
    }

    public void setFiles(List<FileEntry> files) {
        this.files = files;
    }

    /** One entry in the {@code files} array. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FileEntry {

        private String filename;
        private String content;

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
