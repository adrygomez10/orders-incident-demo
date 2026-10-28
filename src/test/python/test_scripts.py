"""Check bounded synthetic traffic without a running order application."""

import importlib.util
import io
import json
import subprocess
import sys
import tempfile
import unittest
import urllib.error
import urllib.request
from contextlib import nullcontext, redirect_stderr, redirect_stdout
from pathlib import Path
from types import SimpleNamespace
from unittest import mock

ROOT = Path(__file__).resolve().parents[3]


def _load(name):
    path = ROOT / "scripts" / f"{name}.py"
    specification = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(specification)
    specification.loader.exec_module(module)
    return module


class TrafficContracts(unittest.TestCase):
    """Keep traffic bounded, measurable and safe to import."""

    def test_imports_do_not_start_traffic_or_docker(self):
        """Importing either CLI must not inspect Git or contact a service."""
        with (
            mock.patch.object(
                subprocess, "check_output", side_effect=AssertionError
            ),
            mock.patch.object(subprocess, "run", side_effect=AssertionError),
            mock.patch.object(
                urllib.request, "urlopen", side_effect=AssertionError
            ),
            mock.patch.object(Path, "mkdir", side_effect=AssertionError),
        ):
            _load("generate_traffic")
            _load("verify_scenario")

    def test_report_retains_inputs_statuses_and_denominator(self):
        """Retain absent and explicit references in the measurement."""
        traffic = _load("generate_traffic")
        bodies = []

        def reply(request, timeout):
            self.assertEqual(10, timeout)
            body = json.loads(request.data)
            bodies.append(body)
            if "customerReference" not in body:
                raise urllib.error.HTTPError(
                    request.full_url, 500, "Controlled failure", None, None
                )
            return nullcontext(SimpleNamespace(status=201))

        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "traffic.json"
            with (
                mock.patch.object(
                    sys,
                    "argv",
                    ["traffic", "--requests", "3", "--output", str(output)],
                ),
                mock.patch.object(
                    traffic.urllib.request, "urlopen", side_effect=reply
                ),
                redirect_stdout(io.StringIO()),
            ):
                traffic.main()
            report = json.loads(output.read_text("utf-8"))
        self.assertEqual(3, report["requests"])
        self.assertEqual(1, report["errors"])
        self.assertEqual(
            [500, 201, 201], [row["status"] for row in report["results"]]
        )
        self.assertNotIn("customerReference", bodies[0])
        self.assertEqual("  ORDER-001  ", bodies[1]["customerReference"])

    def test_invalid_batch_sizes_are_rejected_before_http(self):
        """The CLI rejects batches outside the one-to-one-hundred bound."""
        traffic = _load("generate_traffic")
        for count in (0, 101):
            with (
                mock.patch.object(
                    sys, "argv", ["traffic", "--requests", str(count)]
                ),
                mock.patch.object(
                    traffic.urllib.request,
                    "urlopen",
                    side_effect=AssertionError,
                ),
                redirect_stderr(io.StringIO()),
                self.assertRaises(SystemExit) as failure,
            ):
                traffic.main()
            self.assertEqual(2, failure.exception.code)


if __name__ == "__main__":
    unittest.main()
