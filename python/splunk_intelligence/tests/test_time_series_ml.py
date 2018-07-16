import argparse
import json

import numpy as np

import sys

from TimeSeriesML import TSAnomlyDetector
from sources.FileLoader import FileLoader
from mock import patch
from core.distance.SAXHMMDistance import SAXHMMDistanceFinder

parser = argparse.ArgumentParser()
parser.add_argument("--analysis_minute", type=int, required=True)
parser.add_argument("--time_series_ml_analysis_type", type=str, required=True)
parser.add_argument("--analysis_start_min", type=int, required=True)
parser.add_argument("--analysis_start_time", type=int)
parser.add_argument("--tolerance", type=int, required=True)
parser.add_argument("--smooth_window", type=int, required=True)
parser.add_argument("--min_rpm", type=int, required=True)
parser.add_argument("--comparison_unit_window", type=int, required=True)
parser.add_argument("--parallelProcesses", type=int, required=True)
parser.add_argument('--max_nodes_threshold', nargs='?', const=19, type=int, default=19)

options = parser.parse_args(['--analysis_minute', '29', '--analysis_start_min', 0, '--tolerance', '1', '--smooth_window', '3', '--min_rpm', '10',
                             '--comparison_unit_window', '1', '--parallelProcesses', '1', '--max_nodes_threshold', '19', '--time_series_ml_analysis_type', 'COMPARATIVE'])

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


def test_max_threshold_node():
    """Tests if max_nodes_threshold is not specified, fast method is not called """
    test_options = parser.parse_args(
        ['--analysis_minute', '29', '--analysis_start_min', 0, '--tolerance', '1', '--smooth_window', '3',
         '--min_rpm', '10',
         '--comparison_unit_window', '1', '--parallelProcesses', '1', '--time_series_ml_analysis_type', 'COMPARATIVE'])
    control = FileLoader.load_data('resources/ts/nr_control_live.json')
    test = FileLoader.load_data('resources/ts/nr_test_live.json')
    with patch("TimeSeriesML.TSAnomlyDetector.fast_analysis_metric") as mock_update_in:
        tsa_class = TSAnomlyDetector(test_options, metric_template, control, test)
        tsa_class.analyze()
        assert not mock_update_in.called


def run_analysis(options_var, ctrl_file, test_file, out_file):
    control = FileLoader.load_data(ctrl_file)
    test = FileLoader.load_data(test_file)
    out = FileLoader.load_data(out_file)['transactions']
    out_mod = {}
    for o in out.values():
        out_mod[o['txn_name']] = {'metrics': {}}
        for m in o['metrics'].values():
            out_mod[o['txn_name']]['metrics'][m['metric_name']] = m

    out = out_mod

    anomaly_detector = TSAnomlyDetector(options_var, metric_template, control, test)
    result = anomaly_detector.analyze()
    for txn_id, txn_data in result['transactions'].items():
        assert txn_data['txn_name'] in out
        assert txn_data['txn_tag'] == 'default'
        for metrics_id, metric_data in txn_data['metrics'].items():
            print(txn_data['txn_name'])
            assert metric_data['metric_name'] in out[txn_data['txn_name']]['metrics']
            assert metric_data['max_risk'] == out[txn_data['txn_name']]['metrics'][metric_data['metric_name']][
                'max_risk']
            out_metric_data = out[txn_data['txn_name']]['metrics'][metric_data['metric_name']]['results']
            for host_name, host_data in metric_data['results'].items():
                assert out_metric_data[host_name] is not None
                assert ''.join(host_data['control_cuts']) == ''.join(out_metric_data[host_name]['control_cuts'])
                assert ''.join(host_data['test_cuts']) == ''.join(out_metric_data[host_name]['test_cuts'])
                assert compare(host_data['score'], out_metric_data[host_name]['score'])
                assert host_data['optimal_cuts'] == out_metric_data[host_name]['optimal_cuts']
                assert host_data['control_data'] == out_metric_data[host_name]['control_data']
                assert host_data['test_data'] == out_metric_data[host_name]['test_data']
                assert compare(host_data['risk'], out_metric_data[host_name]['risk'])
                assert host_data['nn'] == out_metric_data[host_name]['nn']


def run_analysis_fast(options_var, ctrl_file, test_file, out_file):
    control = FileLoader.load_data(ctrl_file)
    test = FileLoader.load_data(test_file)
    out = FileLoader.load_data(out_file)['transactions']
    out_mod = {}
    for o in out.values():
        out_mod[o['txn_name']] = {'metrics': {}}
        for m in o['metrics'].values():
            out_mod[o['txn_name']]['metrics'][m['metric_name']] = m

    out = out_mod

    anomaly_detector = TSAnomlyDetector(options_var, metric_template, control, test)
    result = anomaly_detector.analyze()

    for txn_id, txn_data in result['transactions'].items():
        assert txn_data['txn_name'] in out
        assert txn_data['txn_tag'] == 'default'
        for metrics_id, metric_data in txn_data['metrics'].items():
            print(txn_data['txn_name'])
            assert metric_data['metric_name'] in out[txn_data['txn_name']]['metrics']
            assert metric_data['max_risk'] == out[txn_data['txn_name']]['metrics'][metric_data['metric_name']][
                'max_risk']
            out_metric_data = out[txn_data['txn_name']]['metrics'][metric_data['metric_name']]['results']
            for host_name, host_data in metric_data['results'].items():
                assert out_metric_data[host_name] is not None
                assert compare(host_data['score'], out_metric_data[host_name]['score'])
                assert host_data['control_data'] == out_metric_data[host_name]['control_data']
                assert host_data['test_data'] == out_metric_data[host_name]['test_data']
                assert compare(host_data['risk'], out_metric_data[host_name]['risk'])



def run_analysis_2(options_var, ctrl_file, test_file):
    control = FileLoader.load_data(ctrl_file)
    test = FileLoader.load_data(test_file)
    is_error_in_control = 0
    # making sure that control does not have error metric
    for transaction in control:
        if 'error' in transaction['values'].keys():
            is_error_in_control = 1
    anomaly_detector = TSAnomlyDetector(options_var, metric_template, control, test)
    result = anomaly_detector.analyze()
    is_error_in_result = 0
    for txn_id, txn_data in result['transactions'].items():
        for metrics_id, metric_data in txn_data['metrics'].items():
            if metric_data.get('metric_name') == 'error':
                is_error_in_result = 1
                for result in metric_data['results'].values():
                    assert result['control_data'] == [0.0]
    assert is_error_in_control == 0
    assert is_error_in_result == 1




def run_prediction_analysis(options_var, test_file, out_file):
    test = FileLoader.load_data(test_file)
    out = FileLoader.load_data(out_file)['transactions']
    out_mod = {}
    for o in out.values():
        out_mod[o['txn_name']] = {'metrics': {}}
        for m in o['metrics'].values():
            out_mod[o['txn_name']]['metrics'][m['metric_name']] = m

    out = out_mod

    anomaly_detector = TSAnomlyDetector(options_var, metric_template, {}, test)
    result = anomaly_detector.analyze()
    for txn_id, txn_data in result['transactions'].items():
        assert txn_data['txn_name'] in out
        assert txn_data['txn_tag'] == 'default'
        for metrics_id, metric_data in txn_data['metrics'].items():
            print(txn_data['txn_name'])
            assert metric_data['metric_name'] in out[txn_data['txn_name']]['metrics']
            assert metric_data['max_risk'] == out[txn_data['txn_name']]['metrics'][metric_data['metric_name']][
                'max_risk']
            out_metric_data = out[txn_data['txn_name']]['metrics'][metric_data['metric_name']]['results']
            for host_name, host_data in metric_data['results'].items():
                assert out_metric_data[host_name] is not None
                assert compare(host_data['score'], out_metric_data[host_name]['score'])
                assert compare(host_data['risk'], out_metric_data[host_name]['risk'])
                assert host_data['anomalies'] == out_metric_data[host_name]['anomalies']


def test_run_2():
    run_analysis(options, 'resources/ts/nr_control_live.json', 'resources/ts/nr_test_live.json', 'resources/ts/nr_out_live.json')


def test_run_3():
    options_var = parser.parse_args(
        ['--analysis_minute', '2', '--analysis_start_min', 0, '--tolerance', '1', '--smooth_window', '3', '--min_rpm',
         '10',
         '--comparison_unit_window', '1', '--parallelProcesses', '1', '--max_nodes_threshold', '19', '--time_series_ml_analysis_type', 'COMPARATIVE'])
    run_analysis(options_var, 'resources/ts/nr_control_live_3.json', 'resources/ts/nr_test_live_3.json', 'resources/ts/nr_out_live_3.json')


def test_run_4():
    options_var = parser.parse_args(
        ['--analysis_minute', '2', '--analysis_start_min', 0, '--tolerance', '1', '--smooth_window', '3', '--min_rpm',
         '10',
         '--comparison_unit_window', '1', '--parallelProcesses', '1', '--max_nodes_threshold', '19', '--time_series_ml_analysis_type', 'COMPARATIVE'])
    run_analysis(options_var, 'resources/ts/nr_control_live_4.json', 'resources/ts/nr_test_live_4.json', 'resources/ts/nr_out_live_4.json')


def test_run_5():
    options_var = parser.parse_args(
        ['--analysis_minute', '120', '--analysis_start_min', 0, '--tolerance', '1', '--smooth_window', '3', '--min_rpm',
         '10',
         '--comparison_unit_window', '1', '--parallelProcesses', '1', '--max_nodes_threshold', '19', '--time_series_ml_analysis_type', 'PREDICTIVE', '--analysis_start_time', '60'])
    run_prediction_analysis(options_var, 'resources/ts/nr_test_live_prediction.json', 'resources/ts/nr_out_live_prediction.json')

def test_run_6():
    '''testing if test has error metric but control does not, the analysis is done with all zeros time series as control data for error'''
    options_var = parser.parse_args(
        ['--analysis_minute', '2', '--analysis_start_min', 0, '--tolerance', '1', '--smooth_window', '3', '--min_rpm',
         '10',
         '--comparison_unit_window', '1', '--parallelProcesses', '1', '--max_nodes_threshold', '19', '--time_series_ml_analysis_type', 'COMPARATIVE'])
    run_analysis_2(options_var, 'resources/ts/nr_control_live_5.json', 'resources/ts/nr_test_live_5.json')

def test_run_7():
    '''testing fast method'''
    options_var = parser.parse_args(
        ['--analysis_minute', '2', '--analysis_start_min', 0, '--tolerance', '1', '--smooth_window', '3', '--min_rpm',
         '10',
         '--comparison_unit_window', '1', '--parallelProcesses', '1', '--max_nodes_threshold', '1', '--time_series_ml_analysis_type', 'COMPARATIVE'])
    run_analysis_fast(options_var, 'resources/ts/nr_control_live_4.json', 'resources/ts/nr_test_live_4.json', 'resources/ts/nr_out_fast_4.json')

def main(args):
    test_run_2()
    test_run_3()
    test_run_4()
    test_run_5()
    test_run_6()
    test_run_7()




if __name__ == "__main__":
    main(sys.argv)