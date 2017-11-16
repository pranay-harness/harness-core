import sys

import numpy as np

from core.distance.SAXHMMDistance import SAXHMMDistanceFinder, SAXHMMDistance
from core.util.TimeSeriesUtils import get_deviation_type, get_deviation_min_threshold
from sources.SplunkFileSource import SplunkFileSource


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
    txns = SplunkFileSource.load_data(filename)
    for txn_name, txn_data in txns.items():
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
                                       get_deviation_min_threshold(metric_data['metric_name']))

            results = shd.compute_dist()
            # if 'results' in metric_data:
            #     for index, (host, host_data) in enumerate(metric_data['results'].items()):
            #         assert str_equal(host_data['test_cuts'], results['test_cuts'][index])
            #         assert str_equal(host_data['control_cuts'], results['control_cuts'][index])
            #         assert compare(host_data['risk'], results['risk'][index])
            #         assert compare(host_data['score'], results['score'][index])
            #         assert host_data['nn'] == metric_data['control']['host_names'][results['nn'][index]]
            #         assert lists_equal(host_data['distance'], results['distances'][index])
            #         if 'optimal_cuts' in host_data:
            #             assert str_equal(host_data['optimal_cuts'], results['optimal_test_cuts'][index])
            #             assert lists_equal(host_data['optimal_data'], results['optimal_test_data'][index])


def test_1():
    # run_analysis('tests/resources/ts/ts_out_harness_1.json')
    print('hi')


def test_2():
    # run_analysis('tests/resources/ts/ts_out_harness_2.json')
    print('hi')


def test_3():
    # run_analysis('tests/resources/ts/ts_out_harness_3.json', True)
    print('hi')


def test_4():
    # run_analysis('tests/resources/ts/ts_out_harness_4.json')
    print('hi')

def test_5():
    #run_analysis('resources/ts/ts_out_harness_5.json', True)
    print('hi')


def main(args):
    dist = SAXHMMDistance()
    print(dist.distance_matrix)


if __name__ == "__main__":
    main(sys.argv)
