"""Generate bounded synthetic order traffic and retain its denominator."""

import argparse
import datetime
import json
import pathlib
import urllib.error
import urllib.request


def main():
    """Run the CLI command without performing I/O during import."""
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="http://127.0.0.1:18091")
    parser.add_argument("--requests", type=int, default=30)
    parser.add_argument("--output", default="reports/traffic.json")
    args = parser.parse_args()
    if not 1 <= args.requests <= 100:
        parser.error("requests must be between 1 and 100")
    start = datetime.datetime.now(datetime.UTC).isoformat()
    results = []
    for i in range(args.requests):
        legacy = i % 3 == 0
        body = {"quantity": 1}
        if not legacy:
            body["customerReference"] = f"  ORDER-{i:03d}  "
        req = urllib.request.Request(
            args.url + "/orders",
            data=json.dumps(body).encode(),
            headers={
                "Content-Type": "application/json",
                "X-Demo-User": f"demo-user-{i % 5}",
                "X-Client-Version": "legacy-1" if legacy else "web-2",
            },
            method="POST",
        )
        try:
            with urllib.request.urlopen(req, timeout=10) as response:
                status = response.status
        except urllib.error.HTTPError as error:
            status = error.code
        results.append(
            {
                "request": i,
                "client": "legacy-1" if legacy else "web-2",
                "status": status,
            }
        )
    report = {
        "kind": "synthetic-traffic-measurement",
        "since": start,
        "until": datetime.datetime.now(datetime.UTC).isoformat(),
        "requests": len(results),
        "errors": sum(r["status"] >= 500 for r in results),
        "results": results,
    }
    target = pathlib.Path(args.output)
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({k: v for k, v in report.items() if k != "results"}))


if __name__ == "__main__":
    main()
