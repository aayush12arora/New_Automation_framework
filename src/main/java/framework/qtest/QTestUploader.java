package framework.qtest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import framework.constants.FrameworkConstants;
import framework.reporting.ExtentReportManager;
import framework.utilities.LoggerUtil;
import framework.utilities.PropertyManager;
import org.slf4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Pushes test results to qTest's Automation API at the end of a run, so a plain
 * {@code mvn test} is enough — no separate upload command.
 *
 * <p>Two calls happen, in order:</p>
 * <ol>
 *   <li><b>Results</b> — a JUnit-format XML report is built directly from the
 *       in-memory {@link ITestContext} (not read back from Surefire's own report
 *       files, which are written only <em>after</em> this listener runs) and POSTed
 *       to qTest's {@code auto-test-logs} endpoint. That call is documented as
 *       asynchronous: it returns a job id, which is polled until the job finishes.</li>
 *   <li><b>Extent report attachment</b> — once the job reports success, its response
 *       is searched for the created Test Run id, and the Extent HTML report is
 *       attached to that Test Run as supporting evidence.</li>
 * </ol>
 *
 * <p><b>This second step is best-effort.</b> qTest's exact job-response shape and
 * attachment endpoint can vary by tenant/version — this was built without access to
 * a live account. Every response is logged in full, so if the Test Run id can't be
 * found, or the attachment call is rejected, the log shows exactly what qTest
 * returned; feed that back to adjust {@link #findTestRunId(JsonNode)} or the
 * attachment URL.</p>
 *
 * <p>Controlled entirely by {@code framework.properties}: {@code qtestEnabled}
 * (default {@code false}), {@code qtestDomain}, {@code qtestProjectId}. The API
 * token is read from the {@code QTEST_API_TOKEN} environment variable rather than
 * a properties file, so it is never committed to source control.</p>
 */
public final class QTestUploader {

    private static final Logger LOG = LoggerUtil.getLogger(QTestUploader.class);
    // Plain alphanumeric boundary (no leading dashes baked into the token itself) for
    // maximum compatibility with strict server-side multipart parsers.
    private static final String BOUNDARY = "FrameworkQTestBoundary7f3a9c";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final int JOB_POLL_MAX_ATTEMPTS = 15;
    private static final long JOB_POLL_INTERVAL_MS = 2000;

    private QTestUploader() {
        // Prevent instantiation.
    }

    /**
     * Trims stray whitespace and strips a leading {@code "Bearer "} if present, since qTest's
     * "Resources" page exposes several token fields (Token / Bearer Token / Master token) and
     * it isn't obvious in advance whether the copied value already includes the scheme prefix.
     * Every call site adds exactly one {@code "Bearer "} itself, so this avoids sending a
     * doubled-up {@code "Bearer Bearer <token>"} header, which qTest would reject.
     *
     * @return the normalized token, or {@code null} if unset/blank
     */
    private static String normalizeToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }
        String trimmed = rawToken.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            trimmed = trimmed.substring(7).trim();
        }
        return trimmed;
    }

    /** Builds a JUnit XML report from {@code context} and uploads it, if {@code qtestEnabled=true}. */
    public static void uploadIfEnabled(ITestContext context) {
        if (!PropertyManager.getBoolean(FrameworkConstants.QTEST_ENABLED)) {
            return;
        }
        String token = normalizeToken(System.getenv(FrameworkConstants.QTEST_API_TOKEN_ENV_VAR));
        if (token == null) {
            LOG.error("qTest upload skipped: {} environment variable is not set",
                    FrameworkConstants.QTEST_API_TOKEN_ENV_VAR);
            return;
        }

        String domain = PropertyManager.get(FrameworkConstants.QTEST_DOMAIN);
        String projectId = PropertyManager.get(FrameworkConstants.QTEST_PROJECT_ID);

        String xml = buildJunitXml(context);
        Path localCopy = writeLocalCopy(xml);

        Long jobId = uploadJunitXml(xml, localCopy.getFileName().toString(), domain, projectId, token);
        if (jobId == null) {
            return; // Already logged.
        }

        String testRunId = pollJobForTestRunId(domain, projectId, jobId, token);
        if (testRunId == null) {
            LOG.warn("Could not determine the created qTest Test Run id from the automation job "
                    + "(see the job response logged above) — Extent report was not attached. "
                    + "Share that response so findTestRunId() can be adjusted.");
            return;
        }
        attachExtentReport(domain, projectId, testRunId, token);
    }

    // ------------------------------------------------------------ Step 1: results

    private static String buildJunitXml(ITestContext context) {
        Set<ITestResult> passed = context.getPassedTests().getAllResults();
        Set<ITestResult> failed = context.getFailedTests().getAllResults();
        Set<ITestResult> skipped = context.getSkippedTests().getAllResults();
        int total = passed.size() + failed.size() + skipped.size();

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<testsuite name=\"%s\" tests=\"%d\" failures=\"%d\" skipped=\"%d\">\n"
                .formatted(escape(context.getName()), total, failed.size(), skipped.size()));

        passed.forEach(result -> xml.append(testCase(result, null, false)));
        failed.forEach(result -> xml.append(testCase(result, result.getThrowable(), false)));
        skipped.forEach(result -> xml.append(testCase(result, null, true)));

        xml.append("</testsuite>\n");
        return xml.toString();
    }

    private static String testCase(ITestResult result, Throwable failure, boolean wasSkipped) {
        String classname = escape(result.getMethod().getTestClass().getName());
        String name = escape(result.getMethod().getMethodName());
        double seconds = (result.getEndMillis() - result.getStartMillis()) / 1000.0;

        StringBuilder testCase = new StringBuilder();
        testCase.append("  <testcase classname=\"%s\" name=\"%s\" time=\"%.3f\">\n"
                .formatted(classname, name, seconds));
        if (failure != null) {
            testCase.append("    <failure message=\"%s\">%s</failure>\n"
                    .formatted(escape(String.valueOf(failure.getMessage())), escape(stackTraceOf(failure))));
        }
        if (wasSkipped) {
            testCase.append("    <skipped/>\n");
        }
        testCase.append("  </testcase>\n");
        return testCase.toString();
    }

    private static String stackTraceOf(Throwable throwable) {
        StringBuilder trace = new StringBuilder(throwable.toString());
        for (StackTraceElement element : throwable.getStackTrace()) {
            trace.append("\n\tat ").append(element);
        }
        return trace.toString();
    }

    private static String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static Path writeLocalCopy(String xml) {
        Path dir = Path.of(FrameworkConstants.QTEST_REPORT_DIR);
        Path file = dir.resolve("qtest-results_%s.xml".formatted(java.time.LocalDateTime.now().format(TIMESTAMP)));
        try {
            Files.createDirectories(dir);
            Files.writeString(file, xml, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Could not write local copy of qTest report to {}", file, e);
        }
        return file;
    }

    /** @return the qTest job id from the response, or {@code null} if the call itself failed. */
    private static Long uploadJunitXml(String xml, String fileName, String domain, String projectId, String token) {
        String url = "https://%s/api/v3/projects/%s/auto-test-logs?type=junit".formatted(domain, projectId);
        String contentType = "multipart/form-data; boundary=" + BOUNDARY;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", contentType)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(
                            multipartBody(xml.getBytes(StandardCharsets.UTF_8), fileName, "application/xml")))
                    .build();

            LOG.info("qTest auto-test-logs request: POST {} | Content-Type: {}", url, contentType);
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.info("qTest auto-test-logs response: HTTP {} - {}", response.statusCode(), response.body());

            if (response.statusCode() / 100 != 2) {
                LOG.error("qTest results upload failed: HTTP {}", response.statusCode());
                return null;
            }
            JsonNode body = MAPPER.readTree(response.body());
            if (body.has("id")) {
                return body.get("id").asLong();
            }
            LOG.warn("qTest auto-test-logs response had no top-level 'id' (job id) field; cannot poll for completion.");
            return null;
        } catch (IOException | InterruptedException e) {
            LOG.error("qTest results upload failed", e);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    // ------------------------------------------------------------ Step 2: attachment

    /** Polls the automation job until it finishes, then searches the response for a Test Run id. */
    private static String pollJobForTestRunId(String domain, String projectId, long jobId, String token) {
        String url = "https://%s/api/v3/projects/%s/jobs/%d".formatted(domain, projectId, jobId);
        for (int attempt = 1; attempt <= JOB_POLL_MAX_ATTEMPTS; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Bearer " + token)
                        .GET()
                        .build();
                HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode body = MAPPER.readTree(response.body());
                String state = body.path("state").asText("");
                LOG.info("qTest job {} poll {}/{}: state='{}', body={}",
                        jobId, attempt, JOB_POLL_MAX_ATTEMPTS, state, response.body());

                if ("SUCCESS".equalsIgnoreCase(state)) {
                    return findTestRunId(body);
                }
                if ("FAILURE".equalsIgnoreCase(state) || "ERROR".equalsIgnoreCase(state)) {
                    LOG.error("qTest job {} failed: {}", jobId, response.body());
                    return null;
                }
                Thread.sleep(JOB_POLL_INTERVAL_MS);
            } catch (IOException | InterruptedException e) {
                LOG.error("qTest job poll failed", e);
                Thread.currentThread().interrupt();
                return null;
            }
        }
        LOG.warn("qTest job {} did not finish within {} attempts; giving up on attaching the Extent report.",
                jobId, JOB_POLL_MAX_ATTEMPTS);
        return null;
    }

    /**
     * Best-effort search of a completed job's response for the created Test Run id.
     * The exact field name/shape is not confirmed against a live qTest tenant — adjust
     * this against the real "state=SUCCESS" response body logged by {@link #pollJobForTestRunId}.
     */
    private static String findTestRunId(JsonNode job) {
        JsonNode data = job.path("data");
        if (data.isArray() && !data.isEmpty()) {
            data = data.get(0);
        }
        if (data.has("id")) {
            return data.get("id").asText();
        }
        if (job.has("testRunId")) {
            return job.get("testRunId").asText();
        }
        return null;
    }

    private static void attachExtentReport(String domain, String projectId, String testRunId, String token) {
        Path reportPath = ExtentReportManager.getReportPath();
        if (!Files.isRegularFile(reportPath)) {
            LOG.warn("Extent report not found at {}; skipping attachment.", reportPath);
            return;
        }

        String url = "https://%s/api/v3/projects/%s/test-runs/%s/blob-handles".formatted(domain, projectId, testRunId);
        String contentType = "multipart/form-data; boundary=" + BOUNDARY;
        try {
            byte[] reportBytes = Files.readAllBytes(reportPath);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", contentType)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(
                            multipartBody(reportBytes, reportPath.getFileName().toString(), "text/html")))
                    .build();

            LOG.info("qTest attachment request: POST {} | Content-Type: {}", url, contentType);
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 == 2) {
                LOG.info("Attached Extent report to qTest Test Run {} (HTTP {})", testRunId, response.statusCode());
            } else {
                LOG.error("Attaching Extent report to qTest Test Run {} failed: HTTP {} - {}",
                        testRunId, response.statusCode(), response.body());
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("Attaching Extent report to qTest Test Run {} failed", testRunId, e);
            Thread.currentThread().interrupt();
        }
    }

    private static byte[] multipartBody(byte[] content, String fileName, String contentType) throws IOException {
        String header = "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n";
        String footer = "\r\n--" + BOUNDARY + "--\r\n";

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(header.getBytes(StandardCharsets.UTF_8));
        body.write(content);
        body.write(footer.getBytes(StandardCharsets.UTF_8));
        return body.toByteArray();
    }
}
