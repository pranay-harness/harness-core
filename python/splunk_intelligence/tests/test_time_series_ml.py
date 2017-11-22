import argparse
import numpy as np

import sys

from TimeSeriesML import TSAnomlyDetector
from sources.FileLoader import FileLoader

parser = argparse.ArgumentParser()
parser.add_argument("--analysis_minute", type=int, required=True)
parser.add_argument("--tolerance", type=int, required=True)
parser.add_argument("--smooth_window", type=int, required=True)
parser.add_argument("--min_rpm", type=int, required=True)
options = parser.parse_args(['--analysis_minute', '30', '--tolerance', '1', '--smooth_window', '3', '--min_rpm', '10'])


def compare(a, b):
    return abs(float(a) - float(b)) < 0.000001


def test_load_input():
    control = FileLoader.load_data('tests/resources/ts/NRSampleInput.json')
    anomaly_detector = TSAnomlyDetector(options, control, control)
    anomaly_detector.analyze()


def test_run_1():
    control = FileLoader.load_data('tests/resources/ts/NRSampleControl1.json')
    test = FileLoader.load_data('tests/resources/ts/NRSampleTest1.json')
    anomaly_detector = TSAnomlyDetector(options, control, test)
    anomaly_detector.analyze()


def test_run_2():
    control = FileLoader.load_data('tests/resources/ts/nr_control_live.json')
    test = FileLoader.load_data('tests/resources/ts/nr_test_live.json')
    out = FileLoader.load_data('tests/resources/ts/nr_out_live.json')['transactions']
    anomaly_detector = TSAnomlyDetector(options, control, test)
    result = anomaly_detector.analyze()

    for txn_id, txn_data in result['transactions'].items():
        assert txn_data['txn_name'] == out[str(txn_id)]['txn_name']
        for metrics_id, metric_data in txn_data['metrics'].items():
            assert metric_data['metric_name'] == out[str(txn_id)]['metrics'][str(metrics_id)]['metric_name']
            assert metric_data['max_risk'] == out[str(txn_id)]['metrics'][str(metrics_id)]['max_risk']
            out_metric_data = out[str(txn_id)]['metrics'][str(metrics_id)]['results']
            for host_name, host_data in metric_data['results'].items():
                assert out_metric_data[host_name] is not None
                assert ''.join(host_data['control_cuts']) == ''.join(out_metric_data[host_name]['control_cuts'])
                assert ''.join(host_data['test_cuts']) == ''.join(out_metric_data[host_name]['test_cuts'])
                assert compare(host_data['score'], out_metric_data[host_name]['score'])
                assert host_data['optimal_cuts'] == out_metric_data[host_name]['optimal_cuts']
                assert compare(host_data['risk'], out_metric_data[host_name]['risk'])
                assert host_data['nn'] == out_metric_data[host_name]['nn']


def main(args):
    print(np.nanmean([np.nan, np.nan]))
    test_run_2()


if __name__ == "__main__":
    main(sys.argv)