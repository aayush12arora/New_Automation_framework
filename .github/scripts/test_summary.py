"""Builds the e-mail subject and HTML body for a test run.

Reads TestNG's target/surefire-reports/testng-results.xml. A retried test appears once per
attempt, so each test is counted once by its LAST attempt (a test that failed, then passed on
retry, counts as passed).

Usage: python test_summary.py <suite label> <run url> <store url>
Writes: target/email-body.html, and `subject=` / `status=` to $GITHUB_OUTPUT.
"""
import html
import os
import sys
import xml.etree.ElementTree as ET

label, run_url, store_url = (sys.argv[1:] + ["", "", ""])[:3]
results_file = "target/surefire-reports/testng-results.xml"

final = {}  # "Class.method" -> (finished_at, status, message)
if os.path.exists(results_file):
    root = ET.parse(results_file).getroot()
    for cls in root.iter("class"):
        class_name = cls.get("name", "").split(".")[-1]
        for m in cls.iter("test-method"):
            if m.get("is-config") == "true":
                continue
            key = f"{class_name}.{m.get('name')}"
            msg_el = m.find("exception/message")
            message = (msg_el.text or "").strip() if msg_el is not None else ""
            entry = (m.get("finished-at", ""), m.get("status", ""), message)
            if key not in final or entry[0] >= final[key][0]:
                final[key] = entry

passed = sorted(k for k, v in final.items() if v[1] == "PASS")
failed = sorted(k for k, v in final.items() if v[1] == "FAIL")
skipped = sorted(k for k, v in final.items() if v[1] == "SKIP")
total = len(final)

if total == 0:
    status = "ERROR"
    headline = "No test results: the run failed before tests executed (build error or setup failure)."
elif failed:
    status = "FAILED"
    headline = f"{len(failed)} of {total} tests failed."
else:
    status = "PASSED"
    headline = f"All {len(passed)} tests passed" + (f" ({len(skipped)} skipped)." if skipped else ".")

subject = f"[Torsavo {label}] {status}: {len(passed)} passed, {len(failed)} failed, {len(skipped)} skipped"
colour = {"PASSED": "#15803d", "FAILED": "#b91c1c"}.get(status, "#b45309")


def rows(names, with_reason=False):
    out = []
    for n in names:
        reason = html.escape(final[n][2].splitlines()[0][:300]) if with_reason and final[n][2] else ""
        out.append(f"<li><code>{html.escape(n)}</code>" + (f"<br><small style='color:#555'>{reason}</small>" if reason else "") + "</li>")
    return "\n".join(out)


body = f"""<html><body style="font-family:Arial,sans-serif;font-size:14px;color:#111">
<h2 style="color:{colour};margin:0 0 8px">Torsavo {html.escape(label)}: {status}</h2>
<p style="margin:0 0 12px">{html.escape(headline)}</p>
<table cellpadding="6" style="border-collapse:collapse;margin-bottom:12px">
  <tr><td><b>Passed</b></td><td>{len(passed)}</td></tr>
  <tr><td><b>Failed</b></td><td>{len(failed)}</td></tr>
  <tr><td><b>Skipped</b></td><td>{len(skipped)}</td></tr>
  <tr><td><b>Store</b></td><td>{html.escape(store_url or "url from framework.properties")}</td></tr>
</table>
{"<h3>Failed tests</h3><ul>" + rows(failed, True) + "</ul>" if failed else ""}
{"<h3>Skipped tests</h3><ul>" + rows(skipped) + "</ul>" if skipped else ""}
<p>Full HTML report with screenshots: attached as <code>extent-report.zip</code> when it is under 18 MB
(unzip and open <code>index.html</code>). Otherwise download it from the <b>Artifacts</b> section of the run.<br>
Run details and logs: <a href="{html.escape(run_url)}">{html.escape(run_url)}</a></p>
</body></html>"""

os.makedirs("target", exist_ok=True)
with open("target/email-body.html", "w", encoding="utf-8") as f:
    f.write(body)
with open("target/email-subject.txt", "w", encoding="utf-8") as f:
    f.write(subject)

with open(os.environ.get("GITHUB_OUTPUT", os.devnull), "a", encoding="utf-8") as out:
    out.write(f"subject={subject}\nstatus={status}\n")
print(subject)
