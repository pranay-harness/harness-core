import argparse
import json
import sys

from SplunkIntelOptimized import SplunkIntelOptimized
from sources.SplunkDatasetNew import SplunkDatasetNew

options = SplunkIntelOptimized.parse(['--sim_threshold=0.9'])

def test_log_ml_featurizing_fail():
    control_start = 2
    test_start = 2
    print(options)
    prev_out_file = None
    while control_start <= 2 or test_start < 2:

        splunk_dataset = SplunkDatasetNew()

        splunk_dataset.load_prod_file_prev_run(
            '/Users/sriram_parthasarathy/wings/python/splunk_intelligence/data_prod/prev_run/control_unknown_cluster_fail.json',
            [control_start, control_start],
            '/Users/sriram_parthasarathy/wings/python/splunk_intelligence/data_prod/prev_run/test_unknown_cluster_fail.json',
            [test_start, test_start], prev_out_file)

        splunk_intel = SplunkIntelOptimized(splunk_dataset, options)
        splunk_dataset = splunk_intel.run()
        assert len(splunk_dataset.anom_clusters) == 2

        control_start = control_start + 1
        test_start = test_start + 1

def main(args):
    test_log_ml_featurizing_fail()

if __name__ == "__main__":
    main(sys.argv)