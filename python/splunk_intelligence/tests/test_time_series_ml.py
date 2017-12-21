import argparse
import json

import numpy as np

import sys

from TimeSeriesML import TSAnomlyDetector
from sources.FileLoader import FileLoader

parser = argparse.ArgumentParser()
parser.add_argument("--analysis_minute", type=int, required=True)
parser.add_argument("--tolerance", type=int, required=True)
parser.add_argument("--smooth_window", type=int, required=True)
parser.add_argument("--min_rpm", type=int, required=True)
parser.add_argument("--comparison_unit_window", type=int, required=True)
parser.add_argument("--parallelProcesses", type=int, required=True)
parser.add_argument('--max_nodes_threshold', nargs='?', const=19, type=int)

options = parser.parse_args(['--analysis_minute', '29', '--tolerance', '1', '--smooth_window', '3', '--min_rpm', '10',
                             '--comparison_unit_window', '1', '--parallelProcesses', '1', '--max_nodes_threshold', '19'])

metric_template = FileLoader.load_data('resources/ts/metric_template.json')


def compare(a, b):
    return abs(float(a) - float(b)) < 0.000001


def test_load_input():
    control = FileLoader.load_data('resources/ts/NRSampleInput.json')
    anomaly_detector = TSAnomlyDetector(options, metric_template, control, control)
    anomaly_detector.analyze()


def test_run_1():
    control = FileLoader.load_data('resources/ts/NRSampleControl1.json')
    test = FileLoader.load_data('resources/ts/NRSampleTest1.json')
    anomaly_detector = TSAnomlyDetector(options, metric_template, control, test)
    anomaly_detector.analyze()


def test_run_2():
    control = FileLoader.load_data('resources/ts/nr_control_live.json')
    test = FileLoader.load_data('resources/ts/nr_test_live.json')
    out = FileLoader.load_data('resources/ts/nr_out_live.json')['transactions']
    out_mod = {}
    for o in out.values():
        out_mod[o['txn_name']] = {'metrics': {}}
        for m in o['metrics'].values():
            out_mod[o['txn_name']]['metrics'][m['metric_name']] = m

    out = out_mod

    anomaly_detector = TSAnomlyDetector(options, metric_template, control, test)
    result = anomaly_detector.analyze()
    for txn_id, txn_data in result['transactions'].items():
        assert txn_data['txn_name'] in out
        for metrics_id, metric_data in txn_data['metrics'].items():
            assert metric_data['metric_name'] in out[txn_data['txn_name']]['metrics']
            assert metric_data['max_risk'] == out[txn_data['txn_name']]['metrics'][metric_data['metric_name']]['max_risk']
            out_metric_data = out[txn_data['txn_name']]['metrics'][metric_data['metric_name']]['results']
            for host_name, host_data in metric_data['results'].items():
                assert out_metric_data[host_name] is not None
                assert ''.join(host_data['control_cuts']) == ''.join(out_metric_data[host_name]['control_cuts'])
                assert ''.join(host_data['test_cuts']) == ''.join(out_metric_data[host_name]['test_cuts'])
                #assert compare(host_data['score'], out_metric_data[host_name]['score'])
                assert host_data['optimal_cuts'] == out_metric_data[host_name]['optimal_cuts']
                assert compare(host_data['risk'], out_metric_data[host_name]['risk'])
                assert host_data['nn'] == out_metric_data[host_name]['nn']


def main(args):
    test_run_2()


if __name__ == "__main__":
    main(sys.argv)