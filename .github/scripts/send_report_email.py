"""Sends the test-results e-mail from your machine. It is the same e-mail CI sends, so you
can check the SMTP setup before relying on GitHub Actions.

Run test_summary.py first (it writes target/email-body.html), then:

  PowerShell:
    $env:SMTP_USERNAME = "info@torsavo.com"
    $env:SMTP_PASSWORD = "<google app password>"
    python .github/scripts/send_report_email.py

Optional env: REPORT_EMAIL_TO (default aayush12arora@gmail.com), REPORT_EMAIL_FROM
(default = SMTP_USERNAME), SMTP_SERVER (smtp.gmail.com), SMTP_PORT (465), EMAIL_SUBJECT.
The password is read from the environment only; never put it in a file in the repo.
"""
import os
import shutil
import smtplib
import ssl
import sys
import tempfile
from email.message import EmailMessage

user = os.environ.get("SMTP_USERNAME", "")
password = os.environ.get("SMTP_PASSWORD", "")
if not user or not password:
    sys.exit("Set SMTP_USERNAME and SMTP_PASSWORD (a Google app password) first.")

server = os.environ.get("SMTP_SERVER", "smtp.gmail.com")
port = int(os.environ.get("SMTP_PORT", "465"))
sender = os.environ.get("REPORT_EMAIL_FROM", user)
recipients = [r.strip() for r in os.environ.get("REPORT_EMAIL_TO", "aayush12arora@gmail.com").split(",") if r.strip()]

body_path = "target/email-body.html"
if not os.path.exists(body_path):
    sys.exit("target/email-body.html not found: run .github/scripts/test_summary.py first.")
with open(body_path, encoding="utf-8") as f:
    html_body = f.read()

subject = os.environ.get("EMAIL_SUBJECT", "")
if not subject and os.path.exists("target/email-subject.txt"):
    with open("target/email-subject.txt", encoding="utf-8") as f:
        subject = f.read().strip() + " (local)"
subject = subject or "Torsavo test results (local)"

msg = EmailMessage()
msg["Subject"] = subject
msg["From"] = f"Torsavo QA <{sender}>"
msg["To"] = ", ".join(recipients)
msg.set_content("This e-mail needs an HTML-capable client.")
msg.add_alternative(html_body, subtype="html")

# Attach the most recent Extent report (one timestamped folder per run), zipped
reports = "target/extent-report"
runs = sorted(d for d in os.listdir(reports) if os.path.isdir(os.path.join(reports, d))) if os.path.isdir(reports) else []
if runs:
    zip_base = os.path.join(tempfile.mkdtemp(), "extent-report")
    zip_path = shutil.make_archive(zip_base, "zip", reports, runs[-1])
    size_mb = os.path.getsize(zip_path) / 1_048_576
    if size_mb < 18:  # Gmail's limit is 25 MB after base64 encoding (+33%)
        with open(zip_path, "rb") as f:
            msg.add_attachment(f.read(), maintype="application", subtype="zip", filename="extent-report.zip")
        print(f"Attached report {runs[-1]} ({size_mb:.1f} MB)")
    else:
        print(f"Report zip is {size_mb:.1f} MB, too large to attach; sending without it.")

context = ssl.create_default_context()
if port == 465:
    with smtplib.SMTP_SSL(server, port, context=context, timeout=60) as smtp:
        smtp.login(user, password)
        smtp.send_message(msg)
else:
    with smtplib.SMTP(server, port, timeout=60) as smtp:
        smtp.starttls(context=context)
        smtp.login(user, password)
        smtp.send_message(msg)

print(f"Sent '{subject}' from {sender} to {', '.join(recipients)}")
