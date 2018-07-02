import json
import sys

import numpy as np

from core.distance.SAXHMMDistance import SAXHMMDistanceFinder
from sources.FileLoader import FileLoader
from sources.MetricTemplate import MetricTemplate


metric_template = MetricTemplate(FileLoader.load_data('resources/ts/metric_template.json'))


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


def run_analysis(filename, make_nan=False, comparison_unit_window=1):
    txns = FileLoader.load_data(filename)['transactions']
    metric_names = set(metric_template.get_metric_names())
    for txn_id, txn_data in txns.items():
        for metric_name, metric_data in txn_data['metrics'].items():
            #print(txn_data['txn_name'], metric_data['metric_name'])
            if metric_data['metric_name'] not in metric_names:
                continue
            if make_nan:
                create_nan(metric_data['control'])
                create_nan(metric_data['test'])

            shd = SAXHMMDistanceFinder(txn_data['txn_name'],
                                       metric_data['metric_name'], 3, 1,
                                       metric_data['control'],
                                       metric_data['test'],
                                       metric_template, comparison_unit_window)

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
                    if len(results['optimal_test_cuts'][index]) in results:
                        assert str_equal(host_data['optimal_cuts'], results['optimal_test_cuts'][index])
                        assert lists_equal(host_data['optimal_data'], results['optimal_test_data'][index])

#
def test_1():
    run_analysis('resources/ts/nr_out_prod_2.json', True, 1)


def test_2():
    run_analysis('resources/ts/nr_out_prod_1.json', True, 3)

def test_3():
    run_analysis('resources/ts/nr_out_prod_2.json', True, 3)

def test_4():
    run_analysis('resources/ts/nr_out_prod_3.json', True, 1)



def main(args):
    test_3()


if __name__ == "__main__":
    main(sys.argv)
