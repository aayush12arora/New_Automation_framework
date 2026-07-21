package framework.qtest;

import framework.constants.FrameworkConstants;
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
 * <p>The JUnit-format XML qTest expects is built directly from the in-memory
 * {@link ITestContext} results (not read back from Surefire's own report files):
 * Surefire only writes {@code target/surefire-reports/*.xml} <em>after</em> the
 * whole TestNG run returns control to it, which is after this listener's
 * {@code onFinish} already ran — so those files would not exist yet at that point.</p>
 *
 * <p>Controlled entirely by {@code framework.properties}: {@code qtestEnabled}
 * (default {@code false}), {@code qtestDomain}, {@code qtestProjectId}. The API
 * token is read from the {@code QTEST_API_TOKEN} environment variable rather than
 * a properties file, so it is never committed to source control.</p>
 */
public final class QTestUploader {

    private static final Logger LOG = LoggerUtil.getLogger(QTestUploader.class);
    private static final String BOUNDARY = "----FrameworkQTestBoundary";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private QTestUploader() {
        // Prevent instantiation.
    }

    /** Builds a JUnit XML report from {@code context} and uploads it, if {@code qtestEnabled=true}. */
    public static void uploadIfEnabled(ITestContext context) {
        if (!PropertyManager.getBoolean(FrameworkConstants.QTEST_ENABLED)) {
            return;
        }
        String token = System.getenv(FrameworkConstants.QTEST_API_TOKEN_ENV_VAR);
        if (token == null || token.isBlank()) {
            LOG.error("qTest upload skipped: {} environment variable is not set",
                    FrameworkConstants.QTEST_API_TOKEN_ENV_VAR);
            return;
        }

        String xml = buildJunitXml(context);
        Path localCopy = writeLocalCopy(xml);
        upload(xml, localCopy.getFileName().toString(), token);
    }

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

    private static void upload(String xml, String fileName, String token) {
        String domain = PropertyManager.get(FrameworkConstants.QTEST_DOMAIN);
        String projectId = PropertyManager.get(FrameworkConstants.QTEST_PROJECT_ID);
        String url = "https://%s/api/v3/projects/%s/auto-test-logs?type=junit".formatted(domain, projectId);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(buildMultipartBody(xml, fileName)))
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 == 2) {
                LOG.info("Uploaded test results to qTest (HTTP {})", response.statusCode());
            } else {
                LOG.error("qTest upload failed: HTTP {} - {}", response.statusCode(), response.body());
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("qTest upload failed", e);
            Thread.currentThread().interrupt();
        }
    }

    private static byte[] buildMultipartBody(String xml, String fileName) throws IOException {
        String header = "--" + BOUNDARY + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: application/xml\r\n\r\n";
        String footer = "\r\n--" + BOUNDARY + "--\r\n";

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(header.getBytes(StandardCharsets.UTF_8));
        body.write(xml.getBytes(StandardCharsets.UTF_8));
        body.write(footer.getBytes(StandardCharsets.UTF_8));
        return body.toByteArray();
    }
}
