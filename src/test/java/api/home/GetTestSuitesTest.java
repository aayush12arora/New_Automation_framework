package api.home;

import framework.api.client.HomeClient;
import framework.base.BaseApiTest;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * Tests for {@code GET /qe/project/test_suites} — the reference endpoint that exercises the
 * full pattern: positive (contract + data assertions), negative (auth + validation), and
 * edge (pagination boundary).
 *
 * <p>{@code singleThreaded = true} keeps this class's methods on one thread so their WireMock
 * stubs don't collide on the shared mock server (other classes still run in parallel).</p>
 */
@Test(singleThreaded = true)
public class GetTestSuitesTest extends BaseApiTest {

    private static final String PATH = "/qe/project/test_suites";
    private final HomeClient home = new HomeClient();

    @Test(groups = {"smoke", "regression"}, priority = 1)
    public void positiveReturnsTestSuitesWithContract() {
        if (isMock()) {
            stubFor(get(urlPathEqualTo(PATH))
                    .withHeader("Authorization", matching("Bearer .+"))
                    .withQueryParam("status", matching("ALL|ACTIVE|INACTIVE"))
                    .willReturn(okJson("""
                            {"test_suites":[{"id":1,"name":"Suite A","status":"ACTIVE"}],
                             "total":1,"page":1,"page_size":10}""")));
        }

        Response response = home.getTestSuites("ALL", 1, 10, "created_date", "desc", apiData.getProjectId());

        assertions.assertEquals(response.getStatusCode(), 200, "valid request should return 200");
        assertions.assertNotNull(response.jsonPath().getList("test_suites"), "response should contain test_suites");
        assertions.assertEquals(response.jsonPath().getInt("page"), 1, "page should be echoed back");
        assertions.assertEquals(response.jsonPath().getInt("page_size"), 10, "page_size should be echoed back");
    }

    @Test(groups = {"regression"}, priority = 2)
    public void negativeMissingTokenReturns401() {
        if (isMock()) {
            stubFor(get(urlPathEqualTo(PATH))
                    .withHeader("Authorization", absent())
                    .willReturn(aResponse().withStatus(401)));
        }

        Response response = home.getTestSuitesNoAuth("ALL", 1, 10, "created_date", "desc", apiData.getProjectId());

        assertions.assertEquals(response.getStatusCode(), 401, "missing bearer token should return 401");
    }

    @Test(groups = {"regression"}, priority = 3)
    public void negativeInvalidStatusReturns422() {
        if (isMock()) {
            stubFor(get(urlPathEqualTo(PATH))
                    .withHeader("Authorization", matching("Bearer .+"))
                    .willReturn(aResponse().withStatus(422).withHeader("Content-Type", "application/json")
                            .withBody("""
                                    {"detail":[{"loc":["query","status"],
                                     "msg":"value is not a valid enumeration member"}]}""")));
        }

        Response response = home.getTestSuites("INVALID", 1, 10, "created_date", "desc", apiData.getProjectId());

        assertions.assertEquals(response.getStatusCode(), 422, "invalid status enum should return 422");
    }

    @Test(groups = {"regression"}, priority = 4)
    public void edgeMaxPageSizeReturns200() {
        if (isMock()) {
            stubFor(get(urlPathEqualTo(PATH))
                    .withHeader("Authorization", matching("Bearer .+"))
                    .withQueryParam("page_size", equalTo("100"))
                    .willReturn(okJson("""
                            {"test_suites":[],"total":0,"page":1,"page_size":100}""")));
        }

        Response response = home.getTestSuites("ALL", 1, 100, "created_date", "desc", apiData.getProjectId());

        assertions.assertEquals(response.getStatusCode(), 200, "max page_size (100) should be accepted");
        assertions.assertEquals(response.jsonPath().getInt("page_size"), 100, "page_size should be echoed back");
    }
}
