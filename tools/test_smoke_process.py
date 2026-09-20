"""Process cleanup regressions; these tests open no game and no graphical window."""
import os
from pathlib import Path
import signal
import subprocess
import sys
import tempfile
import time
import unittest

from smoke import run_game


class SmokeProcessTests(unittest.TestCase):
    def test_output_is_retained(self):
        with tempfile.TemporaryDirectory() as directory:
            log = Path(directory) / "smoke.log"
            status = run_game([sys.executable, "-c", "print('completed')"], log, timeout=5)
            self.assertEqual(status, 0)
            self.assertIn("completed", log.read_text())

    @unittest.skipUnless(os.name == "posix", "POSIX process signalling")
    def test_timeout_reaps_unresponsive_child(self):
        with tempfile.TemporaryDirectory() as directory:
            log = Path(directory) / "smoke.log"
            code = "import os,signal,time; signal.signal(signal.SIGTERM, signal.SIG_IGN); print(os.getpid(),flush=True); time.sleep(60)"
            with self.assertRaises(subprocess.TimeoutExpired):
                run_game([sys.executable, "-c", code], log, timeout=1, grace=0.1)
            pid = int(log.read_text().strip())
            with self.assertRaises(ProcessLookupError):
                os.kill(pid, 0)

    @unittest.skipUnless(os.name == "posix", "POSIX process signalling")
    def test_cancelling_controller_also_reaps_child(self):
        with tempfile.TemporaryDirectory() as directory:
            log = Path(directory) / "smoke.log"
            child = "import os,signal,time; signal.signal(signal.SIGTERM,signal.SIG_IGN); print(os.getpid(),flush=True); time.sleep(60)"
            controller = "import sys; from smoke import run_game; run_game([sys.executable,'-c'," + repr(child) + "],sys.argv[1],grace=0.1)"
            process = subprocess.Popen([sys.executable, "-c", controller, str(log)],
                                       cwd=str(Path(__file__).parent), stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            try:
                deadline = time.monotonic() + 5
                while (not log.exists() or not log.read_text().strip()) and time.monotonic() < deadline:
                    time.sleep(0.05)
                pid = int(log.read_text().strip())
                process.send_signal(signal.SIGTERM)
                self.assertNotEqual(process.wait(timeout=5), 0)
                with self.assertRaises(ProcessLookupError):
                    os.kill(pid, 0)
            finally:
                if process.poll() is None:
                    process.kill()
                    process.wait()


if __name__ == "__main__":
    unittest.main()
