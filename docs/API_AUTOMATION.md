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
6. [Test data (`customerData`)](#6-test-data)
7. [Endpoint coverage](#7-endpoint-coverage)
8. [Writing / adding an endpoint + test](#8-writing--adding-an-endpoint--test)
9. [Positive / negative / edge scenarios](#9-scenarios)
10. [Logging, reporting & token masking](#10-logging-reporting--masking)
11. [What's shared with the UI framework](#11-shared-with-ui)
12. [Troubleshooting](#12-troubleshooting)

---

## 1. At a glance

| | |
|---|---|
| System under test | `https://agentic.eng.deloitte.com` (`api.baseUrl`) |
| Auth | `ai_assist_token` **session cookie** (a JWT), from the `AI_ASSIST_TOKEN` env var |
| HTTP client | REST Assured |
| Models | Jackson POJOs (`@JsonProperty` for snake_case; `.Builder()` for large bodies) |
| Assertions | shared `assertions` helper (hard + soft) |
| Data | `testdata/apiData.json` → `customerData` (no hard-coding) |
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

The value is read from the **`AI_ASSIST_TOKEN` environment variable** — never committed, never
in `framework.properties`.

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
`TokenManager` reads it fresh on every call, so you only need to update the variable.

---

## 3. How to run

```bash
export AI_ASSIST_TOKEN="<token>"          # required for live calls

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
├── models/
│   ├── request/                request-body POJOs (@JsonProperty snake_case; builders)
│   │   ├── AddTestcasesRequest.java   ReportGenerateRequest.java   RerunRequest.java
│   │   ├── DataCollectorRequest.java  AddDefectsRequest.java       UpdateDefectRequest.java
│   │   ├── CreateSuiteRequest.java  (Builder)   NewDefectRequest.java  (Builder)
│   └── response/
│       └── TestSuitesResponse.java  typed response model (response.as(...))
├── filters/
│   └── LoggingFilter.java      logs request/response + timing to logs + Extent, cookie MASKED
├── config/
│   └── ApiConfig.java          base URI from api.baseUrl
├── auth/
│   └── TokenManager.java       ai_assist_token value from AI_ASSIST_TOKEN env var
└── constants/
    └── ApiEndpoints.java       every endpoint path, one place

src/test/java/api/              tests, one package per group
├── home/HomePageTest.java
├── testsuite/TestSuiteManagementTest.java
├── execution/TestExecutionTest.java
└── defect/DefectManagementTest.java

src/test/resources/testdata/
└── apiData.json               shared API test data → customerData

framework/base/BaseApiTest.java  API test base (extends BaseTest; no browser)
```

**Layering:**
```
Test  →  Service (endpoint method)  →  BaseService (RestAssured + cookie)  →  HTTP
                    ↑ request model (body)              ↓ Response
                                          response.as(Model) / jsonPath  →  assertions
```

---

## 5. Request flow

What happens when a test calls, say, `new HomePageService().getTestSuites(...)`:

1. The **service method** puts the query params into a map and calls `setAuthToken(TokenManager.getToken())`.
2. **`BaseService`** builds a **fresh** `RequestSpecification` for this call:
   `baseUri(api.baseUrl)` + `LoggingFilter` + `Accept: */*` + `Content-Type: application/json`,
   and — because a token is set — `Cookie: ai_assist_token=<jwt>`.
   *(Fresh-per-call means a reused service instance never leaks query params between calls.)*
3. The request is sent; **`LoggingFilter`** logs method/URI (cookie value masked), then the
   response status/time/body — to both the SLF4J log and the Extent report.
4. The test gets a REST Assured `Response`, deserializes it (`response.as(TestSuitesResponse.class)`)
   or reads fields (`response.jsonPath().get("insights")`), and asserts via `assertions`.

Only three things are ever sent: `Accept`, `Content-Type`, and the auth cookie. Browser-noise
headers (`user-agent`, `sec-ch-ua*`, `sec-fetch-*`, `referer`, …) that appear in the Postman
capture are **not** sent — the API doesn't need them.

---

## 6. Test data

API inputs are **not hard-coded**. They live in `src/test/resources/testdata/apiData.json` and
are loaded into `customerData` (the single, shared `CustomerData` object) before each test.
Tests read them with `customerData.get()`:

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

One shared file (not one per test) because the endpoints reuse the same IDs. Switch to Excel by
setting `dataSource=excel` and providing `testdata/apiData.xlsx` (headers = the same field
names). Payload-only content (a defect title, severity, etc.) is passed as literals in the test.

---

## 7. Endpoint coverage

All endpoints from the Postman collection are implemented. Every path lives in `ApiEndpoints`.

### Home Page — `HomePageService`
| Method | Endpoint |
|---|---|
| `getProjects(userId, personaId, moduleId)` | `GET /core-services/v1/project` |
| `getTestSuites(status, page, pageSize, sortBy, sortOrder, projectId)` | `GET /qe/project/test_suites` |
| `getTestSuitesWithoutAuth(...)` | same, no cookie (negative-auth test) |
| `getTestSuitesV2()` | `GET /qe/project/test_suites/v2` |

### Test Suite Management — `TestSuiteService`
| Method | Endpoint |
|---|---|
| `getProjectInsights(projectId)` | `GET /qe/test_suite/project_insights` |
| `getTcmTool(projectId)` | `GET /ftg/v1/functional-testcase/fetch-tcm-tool` |
| `addTestcases(AddTestcasesRequest)` | `POST /qe/test-suite/add-testcases` |
| `deleteTestcase(projectId, testSuiteId, testCaseIds)` | `DELETE /qe/test-suite/delete-testcase` |
| `createSuite(CreateSuiteRequest)` | `POST /qe/test_suite/create` |

### Test Execution — `TestExecutionService`
| Method | Endpoint |
|---|---|
| `getRunId(projectId, testSuiteId)` | `GET /qe/rerun_id` |
| `getRunHistory(userId, projectId, testSuiteId, page, pageSize)` | `GET /qe/test-suite/run-history` |
| `getSuiteDetails(projectId, userId, testSuiteId, pageSize, page, runId)` | `GET /qe/test-suite/details` |
| `generateReport(ReportGenerateRequest)` | `POST /qe/report/generate` |
| `getGithubFiles(projectId, testSuiteId, testCaseId)` | `GET /qe/test-case/github-files` |
| `rerun(RerunRequest)` | `POST /qe/test_suite/rerun` |
| `dataCollectorExecute(projectId, testSuiteId, runId, DataCollectorRequest)` | `POST /data-collector/data-collector/execute` |
| `getTestcaseDetails(projectId, testCaseId, testSuiteId)` | `GET /qe/test-suite/testcase-details` |

### Defect Management — `DefectService`
| Method | Endpoint |
|---|---|
| `addDefects(AddDefectsRequest)` | `POST /qe-defect/defect-management/add_defects` |
| `getDefects(testSuiteId, projectId, page, pageSize)` | `GET /qe-defect/defect-management/get_defects` |
| `getSimilarDefects(testSuiteId, testCaseIds)` | `GET /qe-defect/defect-workflow/get_similar_defects` |
| `getTemplate(testSuiteId, testCaseIds, sessionId, projectName)` | `GET /qe-defect/defect-workflow/get_template` |
| `addNewDefect(projectId, testSuiteId, NewDefectRequest)` | `POST /qe-defect/defect-management/add_new_defect` |
| `updateDefect(UpdateDefectRequest)` | `POST /qe-defect/defect-management/update_defect` |

---

## 8. Writing / adding an endpoint + test

The repeatable pattern — four small steps:

**1) Add the path** to `ApiEndpoints`:
```java
public static final String PROJECT_INSIGHTS = "/qe/test_suite/project_insights";
```

**2) Add a method** to the relevant `*Service`:
```java
public Response getProjectInsights(Integer projectId) {
    setAuthToken(TokenManager.getToken());
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("project_id", projectId);
    return getRequest(ApiEndpoints.PROJECT_INSIGHTS, params);
}
```

**3) Add request/response models** if the endpoint has a body (use a `.Builder()` for large ones):
```java
public class CreateSuiteRequest {
    @JsonProperty("test_suite_name") private final String testSuiteName;
    // ... + static Builder
}
```

**4) Write the test** — `extends BaseApiTest`, read inputs from `customerData`, assert via `assertions`:
```java
public class TestSuiteManagementTest extends BaseApiTest {
    @Test(groups = {"smoke", "regression"})
    public void getProjectInsightsReturnsInsights() {
        Response response = new TestSuiteService().getProjectInsights(customerData.get().getProjectId());
        assertions.assertEquals(response.getStatusCode(), 200, "should return 200");
        assertions.assertNotNull(response.jsonPath().get("insights"), "response should contain insights");
    }
}
```

---

## 9. Scenarios

Each endpoint is designed for three kinds of check (the Home Page group demonstrates all three):

- **Positive** — a valid request returns the expected status **and** 2–3 real fields are asserted
  (not just `status == 200`). Typed model where the contract is defined:
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
  new HomePageService().getTestSuites("INVALID", 1, 10, "created_date", "desc", projectId);
  // assert 422
  ```

To add a no-auth variant for another endpoint, add a sibling method on its service that simply
does **not** call `setAuthToken(...)` — one line, same as `getTestSuitesWithoutAuth`.

---

## 10. Logging, reporting & masking

- **`LoggingFilter`** logs every request (method + URI) and response (status + time + body) to
  the SLF4J log **and** the Extent report, via the shared `StepLogger`.
- The **auth cookie is masked** — the log shows `Cookie ai_assist_token=***MASKED***`, never the
  JWT. Nothing sensitive reaches the logs or the report.
- On failure, the **response body is in the report** next to the failed assertion (evidence).
- API tests appear in the same Extent report as UI tests, and their results are published to
  qTest by the same `TestListener` (when enabled).

---

## 11. Shared with the UI framework

API tests are **not** a separate project — they reuse everything via `BaseApiTest extends BaseTest`:

| Concern | Shared mechanism |
|---|---|
| Reporting | Extent report + `TestListener` |
| Assertions | `assertions` (hard + soft, auto-finalised) |
| Retry | `RetryAnalyzer` (config-gated, `retryEnabled`/`retryCount`) |
| Parallel | `parallel="methods"` in `testng.xml` |
| Groups | `@Test(groups = …)` + `-Dgroups=` |
| Data | `CustomerData` + `dataSource` (json/excel) |
| Config override | `-Dkey=value` beats `framework.properties` |
| qTest publish | `TestListener` → `QTestUploader` |

The only thing API tests don't use is the **WebDriver** — `BaseApiTest` adds no browser.

---

## 12. Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| `IllegalStateException: API auth token missing` | `AI_ASSIST_TOKEN` not set (or set in a different terminal). Export it and re-run. |
| All calls return `401` | Token expired (~30 min) — copy a fresh `ai_assist_token` from DevTools and re-export. |
| Connection timeout / UnknownHost | `agentic.eng.deloitte.com` not reachable from here — connect to the right network/VPN. |
| A field assertion fails but status is right | The response shape differs from the spec — check the logged body and adjust the `jsonPath(...)`/model getter. |
| API tests launch a browser | They shouldn't — make sure the class `extends BaseApiTest`, not `BaseUITest`. |
