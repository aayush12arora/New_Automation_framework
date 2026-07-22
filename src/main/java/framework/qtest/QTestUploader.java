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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Publishes test results to qTest's "Batch Submit Test Logs" automation API at the end of
 * a run, so a plain {@code mvn test} populates qTest with no separate command.
 *
 * <p>Implements the documented endpoint exactly:
 * {@code POST /api/v3/projects/{projectId}/auto-test-logs?type=automation} with a JSON body
 * ({@code Content-Type: application/json}). The body carries a {@code test_cycle} and a
 * {@code test_logs} array; each log supplies the required {@code name}, {@code automation_content},
 * {@code status}, {@code exe_start_date}, {@code exe_end_date} and {@code module_names}.
 * The Extent HTML report is embedded as a base64 {@code attachments} entry on the first test
 * log, so no separate upload call is needed.</p>
 *
 * <p>The endpoint is asynchronous: it returns a job {@code id} and {@code state}
 * ({@code IN_WAITING/IN_PROCESSING/PENDING/FAILED/SUCCESS}), which is then polled until it
 * finishes. Everything is best-effort and non-fatal — a failure here only logs; it never
 * breaks the test run.</p>
 *
 * <p>Config lives in {@code framework.properties}: {@code qtestEnabled}, {@code qtestDomain},
 * {@code qtestProjectId}, {@code qtestTestCycle}. The API token is read from the
 * {@code QTEST_API_TOKEN} environment variable so it is never committed to source control.</p>
 */
public final class QTestUploader {

    private static final Logger LOG = LoggerUtil.getLogger(QTestUploader.class);
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final int JOB_POLL_MAX_ATTEMPTS = 15;
    private static final long JOB_POLL_INTERVAL_MS = 2000;

    private QTestUploader() {
        // Prevent instantiation.
    }

    /** Builds the JSON payload from {@code context} and submits it to qTest, if {@code qtestEnabled=true}. */
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
        String testCycle = PropertyManager.get(FrameworkConstants.QTEST_TEST_CYCLE);
        if (testCycle.isBlank()) {
            LOG.error("qTest upload skipped: '{}' is not set — the auto-test-logs API requires a parent "
                    + "Test Cycle (PID or ID) to place the Test Runs under.", FrameworkConstants.QTEST_TEST_CYCLE);
            return;
        }

        List<Map<String, Object>> testLogs = buildTestLogs(context);
        if (testLogs.isEmpty()) {
            LOG.warn("qTest upload skipped: no test results to submit.");
            return;
        }
        attachExtentReport(testLogs.get(0));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("test_cycle", testCycle);
        payload.put("test_logs", testLogs);

        submit(payload, domain, projectId, token);
    }

    // ---------------------------------------------------------------- payload assembly

    private static List<Map<String, Object>> buildTestLogs(ITestContext context) {
        List<Map<String, Object>> logs = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        // Passed first, so a test that was retried-then-passed isn't also emitted as failed.
        appendLogs(logs, seen, context.getPassedTests().getAllResults(), "PASSED");
        appendLogs(logs, seen, context.getFailedTests().getAllResults(), "FAILED");
        appendLogs(logs, seen, context.getSkippedTests().getAllResults(), "SKIPPED");
        return logs;
    }

    private static void appendLogs(List<Map<String, Object>> logs, Set<String> seen,
                                   Set<ITestResult> results, String status) {
        for (ITestResult result : results) {
            String className = result.getMethod().getTestClass().getName();
            String methodName = result.getMethod().getMethodName();
            String automationContent = className + "#" + methodName;
            if (!seen.add(automationContent)) {
                continue; // Already recorded with a higher-priority status.
            }
            Map<String, Object> log = new LinkedHashMap<>();
            log.put("name", methodName);
            log.put("automation_content", automationContent);
            log.put("status", status);
            log.put("exe_start_date", toIso(result.getStartMillis()));
            log.put("exe_end_date", toIso(result.getEndMillis()));
            log.put("module_names", List.of(simpleName(className)));
            logs.add(log);
        }
    }

    private static String toIso(long epochMillis) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()).format(ISO);
    }

    private static String simpleName(String fullyQualifiedClassName) {
        int lastDot = fullyQualifiedClassName.lastIndexOf('.');
        return lastDot >= 0 ? fullyQualifiedClassName.substring(lastDot + 1) : fullyQualifiedClassName;
    }

    /** Embeds the Extent HTML report as a base64 attachment on the given test log (best-effort). */
    private static void attachExtentReport(Map<String, Object> testLog) {
        Path reportPath = ExtentReportManager.getReportPath();
        if (!Files.isRegularFile(reportPath)) {
            LOG.warn("Extent report not found at {}; skipping attachment.", reportPath);
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(reportPath);
            Map<String, Object> attachment = new LinkedHashMap<>();
            attachment.put("name", reportPath.getFileName().toString());
            attachment.put("content_type", "text/html");
            attachment.put("data", Base64.getEncoder().encodeToString(bytes));
            testLog.put("attachments", List.of(attachment));
        } catch (IOException e) {
            LOG.warn("Could not read Extent report for attachment: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------------- submit + poll

    private static void submit(Map<String, Object> payload, String domain, String projectId, String token) {
        String url = "https://%s/api/v3/projects/%s/auto-test-logs?type=automation".formatted(domain, projectId);
        try {
            String json = MAPPER.writeValueAsString(payload);
            writeLocalCopy(json);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            LOG.info("qTest auto-test-logs request: POST {} ({} test log(s))", url,
                    ((List<?>) payload.get("test_logs")).size());
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            LOG.info("qTest auto-test-logs response: HTTP {} - {}", response.statusCode(), response.body());

            if (response.statusCode() / 100 != 2) {
                LOG.error("qTest results submission failed: HTTP {}", response.statusCode());
                return;
            }
            JsonNode body = MAPPER.readTree(response.body());
            long jobId = body.path("id").asLong();
            LOG.info("qTest accepted submission: job id={}, state={}", jobId, body.path("state").asText());
            if (jobId > 0) {
                pollJob(domain, projectId, jobId, token);
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("qTest results submission failed", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Polls the async job until it reaches a terminal state. The job-status endpoint path is
     * qTest's {@code queue-processing} resource; if this differs on a given tenant the poll just
     * logs what it received and the (already-accepted) submission is unaffected.
     */
    private static void pollJob(String domain, String projectId, long jobId, String token) {
        String url = "https://%s/api/v3/projects/%s/queue-processing/%d".formatted(domain, projectId, jobId);
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
                LOG.info("qTest job {} poll {}/{}: HTTP {}, state='{}', body={}",
                        jobId, attempt, JOB_POLL_MAX_ATTEMPTS, response.statusCode(), state, response.body());

                if ("SUCCESS".equalsIgnoreCase(state)) {
                    LOG.info("qTest job {} completed successfully — results are now in qTest.", jobId);
                    return;
                }
                if ("FAILED".equalsIgnoreCase(state)) {
                    LOG.error("qTest job {} FAILED: {}", jobId, response.body());
                    return;
                }
                Thread.sleep(JOB_POLL_INTERVAL_MS);
            } catch (IOException | InterruptedException e) {
                LOG.error("qTest job poll failed", e);
                Thread.currentThread().interrupt();
                return;
            }
        }
        LOG.warn("qTest job {} still not terminal after {} polls; check the job in qTest directly.",
                jobId, JOB_POLL_MAX_ATTEMPTS);
    }

    // ---------------------------------------------------------------- helpers

    private static void writeLocalCopy(String json) {
        Path dir = Path.of(FrameworkConstants.QTEST_REPORT_DIR);
        Path file = dir.resolve("qtest-payload_%s.json".formatted(java.time.LocalDateTime.now().format(FILE_TS)));
        try {
            Files.createDirectories(dir);
            Files.writeString(file, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("Could not write local copy of qTest payload to {}", file, e);
        }
    }

    /**
     * Trims stray whitespace and strips a leading {@code "Bearer "} if present, since qTest's
     * Resources page exposes several token fields and it isn't obvious whether the copied value
     * already includes the scheme prefix. Each call site adds exactly one {@code "Bearer "}.
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
}
