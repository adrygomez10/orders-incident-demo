"""Verify the functional order baseline with disposable PostgreSQL."""

import datetime
import http.client
import json
import pathlib
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid

ROOT = pathlib.Path(__file__).resolve().parents[1]
PREFIX = "orders-scenario-" + uuid.uuid4().hex[:10]
IMAGE = "orders-incident-demo:verification"


def docker(*command):
    """Run commands against the disposable Docker resources of this check."""
    return (
        subprocess.check_output(
            ["docker", *map(str, command)], stderr=subprocess.STDOUT
        )
        .decode()
        .strip()
    )


def git(*command):
    """Read Git metadata without creating commits or changing state."""
    return (
        subprocess.check_output(["git", "-C", str(ROOT), *command])
        .decode()
        .strip()
    )


def main():
    """Run the CLI command without performing I/O during import."""
    if git("status", "--porcelain", "--untracked-files=normal"):
        raise SystemExit(
            "Use a clean checkout so the recorded release SHA "
            "matches the source being built."
        )
    revision = git("rev-parse", "HEAD")
    report = {
        "kind": "baseline-contract-verification",
        "sourceRevision": revision,
        "sentryEnabled": False,
        "checks": [],
    }
    (ROOT / "reports").mkdir(exist_ok=True)
    network = database = application = False
    try:
        subprocess.run(["docker", "build", "-t", IMAGE, str(ROOT)], check=True)
        docker("network", "create", PREFIX)
        network = True
        docker(
            "run",
            "-d",
            "--name",
            PREFIX + "-db",
            "--network",
            PREFIX,
            "--network-alias",
            "scenario-db",
            "-e",
            "POSTGRES_DB=orders_test",
            "-e",
            "POSTGRES_USER=orders_test",
            "-e",
            "POSTGRES_PASSWORD=test-only",
            "postgres:17.6-alpine",
        )
        database = True
        for _ in range(60):
            try:
                docker(
                    "exec",
                    PREFIX + "-db",
                    "pg_isready",
                    "-U",
                    "orders_test",
                    "-d",
                    "orders_test",
                )
                break
            except subprocess.CalledProcessError:
                time.sleep(1)
        else:
            raise AssertionError("PostgreSQL did not become ready")
        docker(
            "run",
            "-d",
            "--name",
            PREFIX + "-app",
            "--network",
            PREFIX,
            "-p",
            "127.0.0.1::8080",
            "-e",
            "DB_URL=jdbc:postgresql://scenario-db:5432/orders_test",
            "-e",
            "DB_USER=orders_test",
            "-e",
            "DB_PASSWORD=test-only",
            "-e",
            "APP_RELEASE=orders-demo@" + revision,
            "-e",
            "SENTRY_DSN=",
            IMAGE,
        )
        application = True
        port = docker("port", PREFIX + "-app", "8080/tcp").split(":")[-1]
        base = "http://127.0.0.1:" + port
        for _ in range(90):
            try:
                with urllib.request.urlopen(
                    base + "/actuator/health", timeout=2
                ) as response:
                    if response.status == 200:
                        break
            except (
                urllib.error.URLError,
                http.client.RemoteDisconnected,
                TimeoutError,
            ):
                pass
            time.sleep(1)
        else:
            raise AssertionError("Application did not become healthy")
        output = ROOT / "reports" / "scenario-traffic.json"
        subprocess.run(
            [
                sys.executable,
                str(ROOT / "scripts/generate_traffic.py"),
                "--url",
                base,
                "--requests",
                "30",
                "--output",
                str(output),
            ],
            check=True,
        )
        traffic = json.loads(output.read_text(encoding="utf-8"))
        assert traffic["requests"] == 30 and traffic["errors"] == 0, traffic
        for result in traffic["results"]:
            assert result["status"] == 201, result
        rows = int(
            docker(
                "exec",
                PREFIX + "-db",
                "psql",
                "-U",
                "orders_test",
                "-d",
                "orders_test",
                "-Atc",
                "SELECT count(*) FROM orders",
            )
        )
        assert rows == 30, rows
        assert docker("exec", PREFIX + "-app", "id", "-u") == "10001"
        report.update(requests=30, errors=0, savedOrders=rows, success=True)
        report["checks"] = [
            "current checkout built without selecting historical versions",
            "10 missing-reference requests return HTTP 201",
            "20 explicit-reference requests return HTTP 201; "
            "30 orders are persisted",
            "application runs as UID 10001; Sentry disabled",
        ]
    finally:
        report["finishedAt"] = datetime.datetime.now(datetime.UTC).isoformat()
        (ROOT / "reports/scenario-verification.json").write_text(
            json.dumps(report, indent=2) + "\n", encoding="utf-8"
        )
        for present, name in [
            (application, PREFIX + "-app"),
            (database, PREFIX + "-db"),
        ]:
            if present:
                docker("rm", "-f", name)
        if network:
            docker("network", "rm", PREFIX)
    print(json.dumps(report))


if __name__ == "__main__":
    main()
