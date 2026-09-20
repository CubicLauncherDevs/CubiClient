import unittest

from compare_performance import summarize


class PerformanceTests(unittest.TestCase):
    def test_unequal_frametimes(self):
        capture = {"schema": 1, "samples": 2, "frameNanos": [10_000_000, 30_000_000]}
        result = summarize(capture)
        self.assertEqual(result["fps"], 50)
        self.assertEqual(result["p95"], 30)
        self.assertEqual(result["seconds"], 0.04)
        self.assertEqual(capture["frameNanos"], [10_000_000, 30_000_000])

    def test_percentiles_recomputed_from_samples(self):
        result = summarize({"schema": 1, "samples": 100, "averageFps": 99999,
                            "frameNanos": [i * 1_000_000 for i in range(1, 101)]})
        self.assertEqual(result["p95"], 95)
        self.assertEqual(result["p99"], 99)
        self.assertAlmostEqual(result["fps"], 100 / 5.05)

    def test_rejects_truncated_or_invalid_captures(self):
        for capture in ({"schema": 2}, {"schema": 1, "samples": 0, "frameNanos": []},
                        {"schema": 1, "samples": 2, "frameNanos": [100]},
                        {"schema": 1, "samples": 1, "frameNanos": [0]},
                        {"schema": 1, "samples": 1, "frameNanos": [True]},
                        {"schema": 1, "samples": 1, "frameNanos": [float("nan")]}):
            with self.subTest(capture=capture), self.assertRaises(ValueError):
                summarize(capture)


if __name__ == "__main__":
    unittest.main()
