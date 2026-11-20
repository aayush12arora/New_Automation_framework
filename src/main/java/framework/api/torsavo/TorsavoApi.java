package framework.api.torsavo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import framework.constants.FrameworkConstants;
import framework.utilities.LoggerUtil;
import framework.utilities.PropertyManager;
import org.slf4j.Logger;
import org.testng.SkipException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Thin client for the Torsavo backend REST API, used for <b>test setup only</b>
 * (creating throwaway customers, logging in quickly, finding a product that is in stock).
 * The behaviour under test is always driven through the browser.
 *
 * <p>Uses the JDK {@link HttpClient} (thread-safe, so one shared instance serves all
 * parallel tests) and Jackson for JSON.</p>
 */
public final class TorsavoApi {

    private static final Logger LOG = LoggerUtil.getLogger(TorsavoApi.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    /** A logged-in customer: tokens + the user JSON the storefront keeps in localStorage. */
    public record AuthSession(String email, String password, String name,
                              String accessToken, String refreshToken, String userJson) { }

    public record ProductInfo(String id, String name, String slug, double price, int stock,
                              String category, List<String> sizeOptions, List<String> colorOptions) { }

    public record CategoryInfo(String name, String slug, List<String> sizes) { }

    public record HttpResult(int status, String body, String contentType) { }

    private TorsavoApi() {
        // Prevent instantiation.
    }

    // ------------------------------------------------------------------ URLs

    /** Storefront base URL without a trailing slash. */
    public static String siteUrl() {
        return stripSlash(PropertyManager.get(FrameworkConstants.URL));
    }

    /**
     * Backend API base, e.g. {@code http://localhost:8000/api}. Uses {@code apiUrl} when set;
     * otherwise derives it from {@code url}: localhost → port 8000, any other host →
     * {@code https://api.<domain>/api}.
     */
    public static String apiUrl() {
        String configured = PropertyManager.getOrDefault(FrameworkConstants.API_URL, "");
        if (!configured.isBlank()) {
            return stripSlash(configured);
        }
        URI site = URI.create(siteUrl());
        String host = site.getHost();
        if ("localhost".equals(host) || "127.0.0.1".equals(host)) {
            return site.getScheme() + "://" + host + ":8000/api";
        }
        String domain = host.startsWith("www.") ? host.substring(4) : host;
        return "https://api." + domain + "/api";
    }

    // ------------------------------------------------------------------ Auth

    /** Registers a brand-new customer with a unique throwaway e-mail. */
    public static AuthSession registerNewCustomer() {
        String domain = PropertyManager.getOrDefault(FrameworkConstants.TEST_EMAIL_DOMAIN, "example.com");
        String unique = UUID.randomUUID().toString().substring(0, 8);
        String email = "torsavo.qa." + System.currentTimeMillis() + "." + unique + "@" + domain;
        return register("QA Tester", email, "Qa@" + unique + "!");
    }

    public static AuthSession register(String name, String email, String password) {
        HttpResult res = post("/auth/register", Map.of("name", name, "email", email, "password", password), null);
        if (res.status() != 201 && res.status() != 200) {
            throw new IllegalStateException("Registration failed (%d): %s".formatted(res.status(), res.body()));
        }
        LOG.info("Registered test customer {}", email);
        return toSession(res.body(), email, password, name);
    }

    public static AuthSession login(String email, String password) {
        HttpResult res = post("/auth/login", Map.of("email", email, "password", password), null);
        if (res.status() != 200) {
            throw new IllegalStateException("Login failed for %s (%d): %s".formatted(email, res.status(), res.body()));
        }
        return toSession(res.body(), email, password, null);
    }

    /**
     * A customer to run a signed-in test as. Each call returns an <b>isolated</b> account
     * (freshly registered) so parallel tests never share a server-side cart. When
     * {@code createTestData=false}, falls back to the fixed {@code testUserEmail} account,
     * and skips the test if none is configured.
     */
    public static AuthSession customerSession() {
        String email = PropertyManager.getOrDefault(FrameworkConstants.TEST_USER_EMAIL, "");
        String password = PropertyManager.getOrDefault(FrameworkConstants.TEST_USER_PASSWORD, "");
        if (createTestDataAllowed()) {
            return registerNewCustomer();
        }
        if (!email.isBlank() && !password.isBlank()) {
            return login(email, password);
        }
        throw new SkipException("createTestData=false and no testUserEmail/testUserPassword configured");
    }

    /** Admin session, or a skip when no admin credentials are configured. */
    public static AuthSession adminSession() {
        String email = PropertyManager.getOrDefault(FrameworkConstants.ADMIN_EMAIL, "");
        String password = PropertyManager.getOrDefault(FrameworkConstants.ADMIN_PASSWORD, "");
        if (email.isBlank() || password.isBlank()) {
            throw new SkipException("adminEmail/adminPassword not configured — admin test skipped");
        }
        return login(email, password);
    }

    public static boolean createTestDataAllowed() {
        return Boolean.parseBoolean(PropertyManager.getOrDefault(FrameworkConstants.CREATE_TEST_DATA, "true"));
    }

    // ------------------------------------------------------------------ Catalogue

    public static List<ProductInfo> products() {
        JsonNode root = getJson("/products?limit=100");
        List<ProductInfo> list = new ArrayList<>();
        for (JsonNode p : root.path("data")) {
            list.add(new ProductInfo(
                    p.path("id").asText(), p.path("name").asText(), p.path("slug").asText(),
                    p.path("price").asDouble(), p.path("stock").asInt(), p.path("category").asText(),
                    strings(p.path("size_options")), strings(p.path("color_options"))));
        }
        return list;
    }

    public static List<CategoryInfo> categories() {
        JsonNode root = getJson("/categories");
        List<CategoryInfo> list = new ArrayList<>();
        for (JsonNode c : root) {
            list.add(new CategoryInfo(c.path("name").asText(), c.path("slug").asText(), strings(c.path("sizes"))));
        }
        return list;
    }

    /** A published product with stock, preferring the most stock (least likely to sell out mid-run). */
    public static ProductInfo inStockProduct() {
        return products().stream()
                .filter(p -> p.stock() > 0)
                .max((a, b) -> Integer.compare(a.stock(), b.stock()))
                .orElseThrow(() -> new SkipException("No in-stock product in the catalogue"));
    }

    public static ProductInfo outOfStockProduct() {
        return products().stream()
                .filter(p -> p.stock() <= 0)
                .findFirst()
                .orElseThrow(() -> new SkipException("No out-of-stock product in the catalogue"));
    }

    /** Sizes a product offers: its own list, or its category's default size set. */
    public static List<String> effectiveSizes(ProductInfo product) {
        if (!product.sizeOptions().isEmpty()) {
            return product.sizeOptions();
        }
        return categories().stream()
                .filter(c -> c.name().equalsIgnoreCase(product.category()))
                .findFirst()
                .map(CategoryInfo::sizes)
                .orElse(List.of());
    }

    // ------------------------------------------------------------------ Offers, payment modes, A/B tests

    /**
     * What {@code GET /offers} reports: the promotions switched on in the backend env and the
     * payment options (cash on delivery, COD advance, prepaid discount).
     */
    public record OffersInfo(boolean loginCouponPopup, boolean idleCheckoutOffer, boolean bundles,
                             boolean cod, double codAdvance, double prepaidDiscountPercent,
                             JsonNode raw) { }

    /**
     * {@code GET /offers}. Skips the test when the backend under test predates the offers /
     * payment-mode release (Express answers "Route /api/offers not found"), so the suite stays
     * green against older deployments and starts enforcing as soon as the release is live.
     */
    public static OffersInfo offersOrSkip() {
        HttpResult res = getUrl(apiUrl() + "/offers");
        skipIfRouteMissing(res, "/offers");
        JsonNode json = json(res, "/offers");
        JsonNode features = json.path("features");
        JsonNode payment = json.path("payment");
        return new OffersInfo(
                features.path("loginCouponPopup").asBoolean(), features.path("idleCheckoutOffer").asBoolean(),
                features.path("bundles").asBoolean(), payment.path("cod").asBoolean(),
                payment.path("codAdvance").asDouble(), payment.path("prepaidDiscountPercent").asDouble(), json);
    }

    /** Skips when this backend has no {@code /experiments} routes yet (see {@link #offersOrSkip()}). */
    public static void requireExperimentsApi() {
        skipIfRouteMissing(get("/experiments", null), "/experiments");
    }

    /** The signed-in customer's cod_popup A/B group: {@code popup} or {@code control}. */
    public static String codPopupVariant(AuthSession session) {
        HttpResult res = get("/experiments", session.accessToken());
        skipIfRouteMissing(res, "/experiments");
        return json(res, "/experiments").path("codPopup").asText();
    }

    /**
     * A freshly registered customer in the given cod_popup group. The group is a hash of the
     * account ID, so this registers throwaway customers until one lands in it (with a 30% popup
     * share, "popup" takes ~3 tries on average). Skips after {@code maxTries}.
     */
    public static AuthSession customerInCodPopupGroup(String variant, int maxTries) {
        if (!createTestDataAllowed()) {
            throw new SkipException("createTestData=false — finding a '" + variant + "' customer needs new accounts");
        }
        for (int i = 0; i < maxTries; i++) {
            AuthSession session = registerNewCustomer();
            if (variant.equals(codPopupVariant(session))) {
                LOG.info("{} is in cod_popup group '{}'", session.email(), variant);
                return session;
            }
        }
        throw new SkipException("No customer landed in cod_popup group '" + variant + "' after " + maxTries
                + " registrations (EXPERIMENT_COD_POPUP_PERCENT may be 0 or 100)");
    }

    /** Adds a product to the signed-in customer's server-side cart. */
    public static void addToServerCart(AuthSession session, String productId, int quantity, String size) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("productId", productId);
        body.put("quantity", quantity);
        if (size != null && !size.isBlank()) {
            body.put("size", size);
        }
        HttpResult res = post("/cart", body, session.accessToken());
        if (res.status() != 201 && res.status() != 200) {
            throw new IllegalStateException("Add to cart failed (%d): %s".formatted(res.status(), res.body()));
        }
    }

    /** Cancels a test order (best effort: tidies up after tests that create orders). */
    public static void cancelOrderQuietly(AuthSession session, String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return;
        }
        try {
            post("/orders/" + orderId + "/cancel", Map.of(), session.accessToken());
        } catch (RuntimeException e) {
            LOG.warn("Could not cancel test order {}: {}", orderId, e.getMessage());
        }
    }

    // ------------------------------------------------------------------ Raw HTTP

    /** GET an API path (e.g. {@code /offers}), optionally with a bearer token. */
    public static HttpResult get(String path, String bearer) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(apiUrl() + path))
                .timeout(Duration.ofSeconds(60)).GET();
        if (bearer != null) {
            builder.header("Authorization", "Bearer " + bearer);
        }
        return send(builder.build());
    }

    /** POST JSON to an API path, optionally with a bearer token. */
    public static HttpResult postJson(String path, Object body, String bearer) {
        return post(path, body, bearer);
    }

    /** Parses a response body as JSON. */
    public static JsonNode json(HttpResult res, String what) {
        try {
            return MAPPER.readTree(res.body());
        } catch (IOException e) {
            throw new IllegalStateException("Invalid JSON from " + what + " (" + res.status() + "): " + res.body(), e);
        }
    }

    private static void skipIfRouteMissing(HttpResult res, String path) {
        if (res.status() == 404 && res.body().contains("not found") && res.body().contains("Route")) {
            throw new SkipException("Backend at " + apiUrl() + " has no " + path
                    + " route yet (offers / COD / A/B release not deployed)");
        }
    }

    /** GET an absolute URL (used for robots.txt, sitemap.xml etc.). */
    public static HttpResult getUrl(String absoluteUrl) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(absoluteUrl))
                .timeout(Duration.ofSeconds(60)).GET().build();
        return send(request);
    }

    private static JsonNode getJson(String path) {
        HttpResult res = getUrl(apiUrl() + path);
        if (res.status() != 200) {
            throw new IllegalStateException("GET %s failed (%d): %s".formatted(path, res.status(), res.body()));
        }
        try {
            return MAPPER.readTree(res.body());
        } catch (IOException e) {
            throw new IllegalStateException("Invalid JSON from " + path, e);
        }
    }

    private static HttpResult post(String path, Object body, String bearer) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(apiUrl() + path))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)));
            if (bearer != null) {
                builder.header("Authorization", "Bearer " + bearer);
            }
            return send(builder.build());
        } catch (IOException e) {
            throw new IllegalStateException("Could not serialise request body", e);
        }
    }

    private static HttpResult send(HttpRequest request) {
        try {
            HttpResponse<String> res = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            return new HttpResult(res.statusCode(), res.body(),
                    res.headers().firstValue("content-type").orElse(""));
        } catch (IOException e) {
            throw new IllegalStateException("HTTP call failed: " + request.uri(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted calling " + request.uri(), e);
        }
    }

    private static AuthSession toSession(String body, String email, String password, String name) {
        try {
            JsonNode json = MAPPER.readTree(body);
            String access = json.hasNonNull("accessToken") ? json.get("accessToken").asText() : json.path("token").asText();
            return new AuthSession(email, password,
                    name != null ? name : json.path("user").path("name").asText(),
                    access, json.path("refreshToken").asText(), MAPPER.writeValueAsString(json.path("user")));
        } catch (IOException e) {
            throw new IllegalStateException("Invalid auth response: " + body, e);
        }
    }

    private static List<String> strings(JsonNode array) {
        List<String> out = new ArrayList<>();
        array.forEach(n -> out.add(n.asText()));
        return out;
    }

    private static String stripSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
