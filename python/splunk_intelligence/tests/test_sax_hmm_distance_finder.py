import sys

import numpy as np

from core.distance.SAXHMMDistance import SAXHMMDistanceFinder, SAXHMMDistance
from core.util.TimeSeriesUtils import get_deviation_type, get_deviation_min_threshold
from sources.FileLoader import FileLoader


def lists_equal(a, b):
    for ai, bi in zip(a, b):
        if abs(float(ai) - float(bi)) > 0.000001:
            return False
    return True


def str_equal(a, b):
    for ai, bi in zip(a, b):
        if ai != bi:
            return False
    return True


def compare(a, b):
    return abs(float(a) - float(b)) < 0.000001


def create_nan(data):
    for data_list in data['data']:
        for i, d in enumerate(data_list):
            if d == -1:
                data_list[i] = np.nan
    if 'weights' in data:
        for data_list in data['weights']:
            for i, d in enumerate(data_list):
                if d == -1:
                    data_list[i] = np.nan


def run_analysis(filename, make_nan=False):
    txns = FileLoader.load_data(filename)['transactions']
    for txn_id, txn_data in txns.items():
        for metric_name, metric_data in txn_data['metrics'].items():
            if 'verify-email' in txn_data['txn_name']:
                print('hi')
            if make_nan:
                create_nan(metric_data['control'])
                create_nan(metric_data['test'])

            shd = SAXHMMDistanceFinder(metric_name, 3, 1,
                                       metric_data['control'],
                                       metric_data['test'],
                                       get_deviation_type(metric_data['metric_name']),
                                       get_deviation_min_threshold(metric_data['metric_name']), 1)

            results = shd.compute_dist()
            if 'results' in metric_data:
                for index, (host, host_data) in enumerate(metric_data['results'].items()):
                    assert str_equal(host_data['test_cuts'], results['test_cuts'][index])
                    assert str_equal(host_data['control_cuts'], results['control_cuts'][index])
                    if host_data['risk'] == -1:
                        for metric_id, metric_values in  txn_data['metrics'].items():
                            if 'requestsPerMinute' in metric_values['metric_name']:
                                break
                        if np.nansum(metric_values['test']['data'][index]) < 10:
                            assert compare(host_data['risk'], "-1")
                        else:
                            assert False
                    else:
                        assert compare(host_data['risk'], results['risk'][index])
                    assert compare(host_data['score'], results['score'][index])
                    assert host_data['nn'] == metric_data['control']['host_names'][results['nn'][index]]
                    assert lists_equal(host_data['distance'], results['distances'][index])
                    if 'optimal_cuts' in host_data:
                        assert str_equal(host_data['optimal_cuts'], results['optimal_test_cuts'][index])
                        assert lists_equal(host_data['optimal_data'], results['optimal_test_data'][index])


def test_1():
    run_analysis('tests/resources/ts/nr_out_live.json', True)


def main(args):
    test_1()


if __name__ == "__main__":
    main(sys.argv)
