# API Automation Guide

API test automation for the **QE Core Services** APIs (`agentic.eng.deloitte.com`), built into
the same framework as the UI tests. This is the API-specific companion to the main
[`README.md`](../README.md); it covers only the API layer — how it's structured, how auth
works, how to run it, and how to add endpoints.

> Built on **REST Assured + TestNG + Jackson**, following a service + typed-model convention
> (base service wrapper → per-group services → typed request/response models → tests).

---

## Table of contents

1. [At a glance](#1-at-a-glance)
2. [Authentication (the `ai_assist_token` cookie)](#2-authentication)
3. [How to run](#3-how-to-run)
4. [Architecture & package layout](#4-architecture--package-layout)
5. [Request flow (what happens on a call)](#5-request-flow)
6. [Test data (one file per test, `customerData`)](#6-test-data)
7. [Response models — one POJO per endpoint](#7-response-models)
8. [Endpoint coverage](#8-endpoint-coverage)
9. [Writing / adding an endpoint + test](#9-writing--adding-an-endpoint--test)
10. [Positive / negative / edge scenarios](#10-scenarios)
11. [Logging, reporting & token masking](#11-logging-reporting--masking)
12. [What's shared with the UI framework](#12-shared-with-ui)
13. [Troubleshooting](#13-troubleshooting)

---

## 1. At a glance

| | |
|---|---|
| System under test | `https://agentic.eng.deloitte.com` (`api.baseUrl`) |
| Auth | `ai_assist_token` **session cookie** (a JWT), read once per test into `customerData.token` |
| HTTP client | REST Assured |
| Models | Jackson POJOs — a **typed request and/or response model for every endpoint** |
| Assertions | shared `assertions` helper (hard + soft) |
| Data | `testdata/<testMethodName>.json` → `customerData` — **one file per test**, same as UI |
| Reporting | Extent + SLF4J logs (token masked), shared with UI |
| Groups | `smoke`, `regression` (run with `-Dgroups=`) |

Run only the API tests:
```bash
export AI_ASSIST_TOKEN="<ai_assist_token cookie value>"
mvn test -Dtest="api.*.*Test"
```

---

## 2. Authentication

The API does **not** use an `Authorization: Bearer` header. It authenticates with the
**`ai_assist_token` session cookie** (a JWT). Every request the framework sends carries:

```
Cookie: ai_assist_token=<jwt>
```

### The token lives on `CustomerData`, set once by `BaseApiTest`

Since **every** API test needs the token, it isn't fetched inside each service call. Instead:

1. `CustomerData` has a `token` field (only field **not** loaded from a data file).
2. `BaseTest.baseSetUp` (superclass `@BeforeMethod`) loads that test's data file into `customerData` first.
3. `BaseApiTest.setApiToken` (subclass `@BeforeMethod`, runs **after**) reads the env var via
   `TokenManager.getToken()` and sets it on the *same* `customerData` instance:
   ```java
   @BeforeMethod(alwaysRun = true)
   public void setApiToken() {
       customerData.get().setToken(TokenManager.getToken());
   }
   ```
4. Tests/services never call `TokenManager` themselves — they read `customerData.get().getToken()`
   and pass it into the service method, exactly like every other field:
   ```java
   CustomerData data = customerData.get();
   Response response = new HomePageService().getTestSuites(
           data.getToken(), "ALL", 1, 10, "created_date", "desc", data.getProjectId());
   ```

This means `TokenManager.getToken()` (and therefore the env-var read / missing-token check) is
called in exactly **one place** in the whole framework.

### Getting the token from the UI

The cookie is `HttpOnly`, so the Console (`document.cookie`) can't read it — copy it from
DevTools:

1. Log into `https://agentic.eng.deloitte.com` (SSO).
2. Open **DevTools (F12) → Application → Storage → Cookies → `https://agentic.eng.deloitte.com`**.
3. Click the **`ai_assist_token`** row and copy its **Value** (the full `eyJ…` JWT).
4. Export it:
   ```bash
   # macOS / Linux
   export AI_ASSIST_TOKEN="eyJhbGciOi...."
   ```
   ```powershell
   # Windows PowerShell (new terminal picks it up)
   setx AI_ASSIST_TOKEN "eyJhbGciOi...."
   ```

The token is **short-lived (~30 min)** — refresh the env var when calls start returning `401`.
Since it's read fresh every test (`setApiToken` runs before each `@Test`), updating the variable
is all that's needed.

---

## 3. How to run

```bash
export AI_ASSIST_TOKEN="<token>"          # required — every API test needs it, see §2

mvn test -Dtest="api.*.*Test"             # all API tests
mvn test -Dtest="api.home.HomePageTest"   # one group
mvn test -Dgroups=smoke -Dtest="api.*.*Test"   # only smoke API tests
```

Override config per run without editing files (system properties beat `framework.properties`):
```bash
mvn test -Dapi.baseUrl=https://other-host -Dtest="api.*.*Test"
```

> **Reachability:** these tests call the real server, so they need `AI_ASSIST_TOKEN` set **and**
> `agentic.eng.deloitte.com` reachable from where you run (VPN/network). There is no mock, so in
> an environment that can't reach the host the tests compile but won't pass.

---

## 4. Architecture & package layout

```
src/main/java/framework/api/
├── base/
│   └── BaseService.java        RestAssured wrapper: base URI, cookie auth, get/post/put/delete
├── services/
│   ├── HomePageService.java        projects, test_suites (+ v2)
│   ├── TestSuiteService.java       insights, tcm-tool, add/delete-testcase, create
│   ├── TestExecutionService.java   run-id, run-history, details, report, github-files, rerun, …
│   └── DefectService.java          add/get defects, similar, template, add-new, update
│   (every method's FIRST parameter is `token`, sourced by the test from customerData.get().getToken())
├── models/
│   ├── request/                request-body POJOs (@JsonProperty snake_case; builders)
│   │   ├── AddTestcasesRequest.java   ReportGenerateRequest.java   RerunRequest.java
│   │   ├── DataCollectorRequest.java  AddDefectsRequest.java       UpdateDefectRequest.java
│   │   └── CreateSuiteRequest.java (Builder)   NewDefectRequest.java (Builder)
│   └── response/                one typed model PER ENDPOINT RESPONSE — see §7
├── filters/
│   └── LoggingFilter.java      logs request/response + timing to logs + Extent, cookie MASKED
├── config/
│   └── ApiConfig.java          base URI from api.baseUrl
├── auth/
│   └── TokenManager.java       ai_assist_token value from AI_ASSIST_TOKEN env var — called
│                                ONLY from BaseApiTest, nowhere else
└── constants/
    └── ApiEndpoints.java       every endpoint path, one place

src/test/java/api/              tests, one package per group
├── home/HomePageTest.java
├── testsuite/TestSuiteManagementTest.java
├── execution/TestExecutionTest.java
└── defect/DefectManagementTest.java

src/test/resources/testdata/
└── <testMethodName>.json      ONE FILE PER TEST — same convention the UI side uses

framework/base/BaseApiTest.java  API test base (extends BaseTest; sets the token; no browser)
```

**Layering:**
```
Test  →  Service (endpoint method, token as 1st param)  →  BaseService (RestAssured + cookie)  →  HTTP
                    ↑ request model (body)                                   ↓ Response
                                          response.as(ResponseModel.class)  →  assertions
```

---

## 5. Request flow

What happens when a test calls, say, `new HomePageService().getTestSuites(token, ...)`:

1. **`BaseApiTest.setApiToken`** has already put the token on `customerData` before the test body ran.
2. The **test** reads `customerData.get().getToken()` and passes it as the service call's first argument.
3. The **service method** puts the query params into a map and calls `setAuthToken(token)`.
4. **`BaseService`** builds a **fresh** `RequestSpecification` for this call:
   `baseUri(api.baseUrl)` + `LoggingFilter` + `Accept: */*` + `Content-Type: application/json`,
   and — because a token was set — `Cookie: ai_assist_token=<jwt>`.
   *(Fresh-per-call means a reused service instance never leaks query params between calls.)*
5. The request is sent; **`LoggingFilter`** logs method/URI (cookie value masked), then the
   response status/time/body — to both the SLF4J log and the Extent report.
6. The test deserializes the response into its typed model (`response.as(XResponse.class)`) and
   asserts via `assertions`.

Only three things are ever sent: `Accept`, `Content-Type`, and the auth cookie. Browser-noise
headers (`user-agent`, `sec-ch-ua*`, `sec-fetch-*`, `referer`, …) seen in a Postman capture are
**not** sent — the API doesn't need them.

---

## 6. Test data

API inputs are **not hard-coded**, and each test has **its own data file**, named after the test
method — the exact same convention the UI side uses:

```
src/test/resources/testdata/
├── getProjectsReturnsProjects.json
├── getTestSuitesReturnsList.json
├── addTestcasesToSuite.json
├── createTestSuite.json
├── getRunIdReturnsRunId.json
├── ... (one .json per @Test method)
```

Each file loads into `customerData` (`BaseTest.baseSetUp`) before the test runs. A typical file:

```json
{
  "userId": "ukiranp@deloitte.com",
  "personaId": 2,
  "moduleId": 1,
  "projectId": 22,
  "testSuiteId": 44,
  "testCaseId": "TBDAA-T601",
  "testCaseIds": ["TBDAA-T601", "TBDAA-T604", "TBDAA-T608", "TBDAA-T610"],
  "runId": "RUN-20260101-001",
  "sessionId": "27de5c8c-af90-40f8-89ef-2a7a02470da0",
  "projectName": "TESTING_TEAM",
  "page": 1,
  "pageSize": 10
}
```

Tests read fields with `customerData.get()`:
```java
CustomerData data = customerData.get();
new HomePageService().getTestSuites(data.getToken(), "ALL", 1, 10, "created_date", "desc", data.getProjectId());
```

**`token` is never in a data file** — it's set by `BaseApiTest` from the environment (§2). Switch
to Excel by setting `dataSource=excel` and providing a matching `<testMethodName>.xlsx` (headers
= the same field names).

---

## 7. Response models

Every endpoint response has its own typed model under `framework.api.models.response`, deserialized in
the test with `response.as(Model.class)` and asserted on typed getters (not raw `jsonPath` strings).
Structurally-identical responses share one model rather than duplicating an identical class
(e.g. `add-testcases` / `delete-testcase` / `update_defect` are all `{success, message}` →
`SuccessMessageResponse`).

| Model | Used by | Shape |
|---|---|---|
| `ProjectsResponse` | `getProjects` | `{ projects: [] }` |
| `TestSuitesResponse` | `getTestSuites` | `{ test_suites: [], total, page, page_size }` |
| `TestSuitesV2Response` | `getTestSuitesV2` | best-effort — endpoint has no documented schema |
| `ProjectInsightsResponse` | `getProjectInsights` | `{ insights: {} }` |
| `TcmToolResponse` | `getTcmTool` | `{ tool, configuration: {} }` |
| `SuccessMessageResponse` | `addTestcases`, `deleteTestcase`, `updateDefect` | `{ success, message }` |
| `CreateSuiteResponse` (+ nested `Data`) | `createSuite` | `{ data: { test_suite_id, message } }` |
| `RunIdResponse` | `getRunId` | `{ run_id }` |
| `RunHistoryResponse` | `getRunHistory` | `{ history: [], total }` |
| `SuiteDetailsResponse` | `getSuiteDetails` | `{ details: {} }` |
| `ReportGenerateResponse` | `generateReport` | `{ report_url, status }` |
| `GithubFilesResponse` (+ nested `FileEntry`) | `getGithubFiles` | `{ files: [{ filename, content }] }` |
| `RerunResponse` | `rerun` | `{ status, new_run_id }` |
| `DataCollectorResponse` | `dataCollectorExecute` | `{ status, message }` |
| `TestcaseDetailsResponse` (+ nested `TestCase`) | `getTestcaseDetails` | `{ test_case: { id, name, status, details } }` |
| `AddDefectsResponse` | `addDefects` | `{ success, defect_ids: [] }` |
| `GetDefectsResponse` | `getDefects` | `{ defects: [], total, page }` |
| `SimilarDefectsResponse` (+ nested `SimilarDefect`) | `getSimilarDefects` | `{ similar_defects: [{ defect_id, similarity_score }] }` |
| `TemplateResponse` | `getTemplate` | `{ template: {} }` |
| `NewDefectResponse` | `addNewDefect` | `{ defect_id, message }` |

Free-form objects with no documented schema (`insights`, `configuration`, `details`, `template`)
map to `Map<String, Object>` rather than a further nested POJO — still typed, just flexible.

All models were verified offline (deserializing representative JSON, including the nested ones —
`CreateSuiteResponse.Data`, `SimilarDefectsResponse.SimilarDefect`, `TestcaseDetailsResponse.TestCase`,
`GithubFilesResponse.FileEntry` — all map correctly).

---

## 8. Endpoint coverage

All endpoints from the Postman collection are implemented. Every path lives in `ApiEndpoints`,
every response has a model (§7).

### Home Page — `HomePageService`
| Method | Endpoint | Response model |
|---|---|---|
| `getProjects(token, userId, personaId, moduleId)` | `GET /core-services/v1/project` | `ProjectsResponse` |
| `getTestSuites(token, status, page, pageSize, sortBy, sortOrder, projectId)` | `GET /qe/project/test_suites` | `TestSuitesResponse` |
| `getTestSuitesWithoutAuth(...)` (no token param) | same, no cookie (negative-auth test) | — |
| `getTestSuitesV2(token)` | `GET /qe/project/test_suites/v2` | `TestSuitesV2Response` |

### Test Suite Management — `TestSuiteService`
| Method | Endpoint | Response model |
|---|---|---|
| `getProjectInsights(token, projectId)` | `GET /qe/test_suite/project_insights` | `ProjectInsightsResponse` |
| `getTcmTool(token, projectId)` | `GET /ftg/v1/functional-testcase/fetch-tcm-tool` | `TcmToolResponse` |
| `addTestcases(token, AddTestcasesRequest)` | `POST /qe/test-suite/add-testcases` | `SuccessMessageResponse` |
| `deleteTestcase(token, projectId, testSuiteId, testCaseIds)` | `DELETE /qe/test-suite/delete-testcase` | `SuccessMessageResponse` |
| `createSuite(token, CreateSuiteRequest)` | `POST /qe/test_suite/create` | `CreateSuiteResponse` |

### Test Execution — `TestExecutionService`
| Method | Endpoint | Response model |
|---|---|---|
| `getRunId(token, projectId, testSuiteId)` | `GET /qe/rerun_id` | `RunIdResponse` |
| `getRunHistory(token, userId, projectId, testSuiteId, page, pageSize)` | `GET /qe/test-suite/run-history` | `RunHistoryResponse` |
| `getSuiteDetails(token, projectId, userId, testSuiteId, pageSize, page, runId)` | `GET /qe/test-suite/details` | `SuiteDetailsResponse` |
| `generateReport(token, ReportGenerateRequest)` | `POST /qe/report/generate` | `ReportGenerateResponse` |
| `getGithubFiles(token, projectId, testSuiteId, testCaseId)` | `GET /qe/test-case/github-files` | `GithubFilesResponse` |
| `rerun(token, RerunRequest)` | `POST /qe/test_suite/rerun` | `RerunResponse` |
| `dataCollectorExecute(token, projectId, testSuiteId, runId, DataCollectorRequest)` | `POST /data-collector/data-collector/execute` | `DataCollectorResponse` |
| `getTestcaseDetails(token, projectId, testCaseId, testSuiteId)` | `GET /qe/test-suite/testcase-details` | `TestcaseDetailsResponse` |

### Defect Management — `DefectService`
| Method | Endpoint | Response model |
|---|---|---|
| `addDefects(token, AddDefectsRequest)` | `POST /qe-defect/defect-management/add_defects` | `AddDefectsResponse` |
| `getDefects(token, testSuiteId, projectId, page, pageSize)` | `GET /qe-defect/defect-management/get_defects` | `GetDefectsResponse` |
| `getSimilarDefects(token, testSuiteId, testCaseIds)` | `GET /qe-defect/defect-workflow/get_similar_defects` | `SimilarDefectsResponse` |
| `getTemplate(token, testSuiteId, testCaseIds, sessionId, projectName)` | `GET /qe-defect/defect-workflow/get_template` | `TemplateResponse` |
| `addNewDefect(token, projectId, testSuiteId, NewDefectRequest)` | `POST /qe-defect/defect-management/add_new_defect` | `NewDefectResponse` |
| `updateDefect(token, UpdateDefectRequest)` | `POST /qe-defect/defect-management/update_defect` | `SuccessMessageResponse` |

---

## 9. Writing / adding an endpoint + test

The repeatable pattern:

**1) Add the path** to `ApiEndpoints`:
```java
public static final String PROJECT_INSIGHTS = "/qe/test_suite/project_insights";
```

**2) Add a response model** (and a request model, if the endpoint has a body) under `models/`:
```java
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectInsightsResponse {
    private Map<String, Object> insights;
    // getter/setter
}
```

**3) Add a method** to the relevant `*Service` — `token` is always the first parameter:
```java
public Response getProjectInsights(String token, Integer projectId) {
    setAuthToken(token);
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("project_id", projectId);
    return getRequest(ApiEndpoints.PROJECT_INSIGHTS, params);
}
```

**4) Add a data file** named exactly after the test method:
`src/test/resources/testdata/<testMethodName>.json` with the fields that test needs (never `token`).

**5) Write the test** — `extends BaseApiTest`, read the token + fields from `customerData.get()`,
deserialize the response, assert via `assertions`:
```java
public class TestSuiteManagementTest extends BaseApiTest {
    @Test(groups = {"smoke", "regression"})
    public void getProjectInsightsReturnsInsights() {
        CustomerData data = customerData.get();
        Response response = new TestSuiteService().getProjectInsights(data.getToken(), data.getProjectId());
        ProjectInsightsResponse body = response.as(ProjectInsightsResponse.class);

        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.assertNotNull(body.getInsights(), "response should contain insights");
    }
}
```

---

## 10. Scenarios

Each endpoint is designed for three kinds of check (the Home Page group demonstrates all three):

- **Positive** — a valid request returns the expected status **and** the typed response model's
  fields are asserted (not just `status == 200`):
  ```java
  TestSuitesResponse body = response.as(TestSuitesResponse.class);
  assertions.assertEquals(body.getPageSize(), 10, "page_size echoed");
  ```
- **Negative (auth)** — call without the cookie → `401`:
  ```java
  new HomePageService().getTestSuitesWithoutAuth("ALL", 1, 10, "created_date", "desc", projectId);
  // assert 401
  ```
- **Edge / validation** — a bad enum/param → `422`:
  ```java
  new HomePageService().getTestSuites(token, "INVALID", 1, 10, "created_date", "desc", projectId);
  // assert 422
  ```

To add a no-auth variant for another endpoint, add a sibling method on its service that simply
doesn't take a `token` parameter and never calls `setAuthToken(...)` — one method, same as
`getTestSuitesWithoutAuth`.

---

## 11. Logging, reporting & masking

- **`LoggingFilter`** logs every request (method + URI) and response (status + time + body) to
  the SLF4J log **and** the Extent report, via the shared `StepLogger`.
- The **auth cookie is masked** — the log shows `Cookie ai_assist_token=***MASKED***`, never the
  JWT. Nothing sensitive reaches the logs or the report.
- On failure, the **response body is in the report** next to the failed assertion (evidence).
- API tests appear in the same Extent report as UI tests, and their results are published to
  qTest by the same `TestListener` (when enabled).

---

## 12. Shared with the UI framework

API tests are **not** a separate project — they reuse everything via `BaseApiTest extends BaseTest`:

| Concern | Shared mechanism |
|---|---|
| Reporting | Extent report + `TestListener` |
| Assertions | `assertions` (hard + soft, auto-finalised) |
| Retry | `RetryAnalyzer` (config-gated, `retryEnabled`/`retryCount`) — verified working for API tests too |
| Parallel | `parallel="methods"` in `testng.xml` |
| Groups | `@Test(groups = …)` + `-Dgroups=` |
| Data | `CustomerData` + `dataSource` (json/excel), one file per test |
| Config override | `-Dkey=value` beats `framework.properties` |
| qTest publish | `TestListener` → `QTestUploader` |

The only differences: `BaseApiTest` adds **no WebDriver**, and adds the one API-only step of
setting `customerData.token` (§2).

---

## 13. Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| `IllegalStateException: API auth token missing` | `AI_ASSIST_TOKEN` not set (or set in a different terminal). Export it and re-run. |
| All calls return `401` | Token expired (~30 min) — copy a fresh `ai_assist_token` from DevTools and re-export. |
| Connection timeout / UnknownHost | `agentic.eng.deloitte.com` not reachable from here — connect to the right network/VPN. |
| A field assertion fails but status is right | The response shape differs from the spec/model — check the logged body and adjust the response model's fields. |
| `NullPointerException` reading `customerData.get()...` | The per-test JSON file is missing or misnamed — it must exactly match the `@Test` method name. |
| API tests launch a browser | They shouldn't — make sure the class `extends BaseApiTest`, not `BaseUITest`. |
