# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import json
import sys
from SplunkIntelOptimized import SplunkIntelOptimized
from sources.FileLoader import FileLoader
from sources.LogCorpus import LogCorpus

options = SplunkIntelOptimized.parse(['--sim_threshold=0.9'])


def test_log_ml_cluster_fail():
    control_start = 2
    test_start = 2
    print(options)
    prev_out_file = None
    while control_start <= 2 or test_start < 2:
        corpus = LogCorpus()

        corpus.load_prod_file_prev_run(
            'resources/logs/control_unknown_cluster_fail.json',
            [control_start, control_start],
            'resources/logs/test_unknown_cluster_fail.json',
            [test_start, test_start], prev_out_file)

        splunk_intel = SplunkIntelOptimized(corpus, options)
        corpus = splunk_intel.run()
        assert len(corpus.anom_clusters) == 2

        control_start = control_start + 1
        test_start = test_start + 1


# Uses the analysis output to setup a test case
def test_log_ml_out_1():
    data = FileLoader.load_data('resources/logs/log_ml_out_1.json')
    control = data['control_events']
    test = data['test_events']
    unknown = data['unknown_events']
    corpus = LogCorpus()
    for events in control.values():
        for event in events:
            corpus.add_event(event, 'control_prev')

    for events in test.values():
        for event in events:
            corpus.add_event(event, 'test_prev')

    sio = SplunkIntelOptimized(corpus, SplunkIntelOptimized.parse(['--sim_threshold', '0.9']))
    result = sio.run()
    assert len(result.anom_clusters) == 2


# # Uses the analysis output to setup a test case
def test_log_ml_out_2():
    data = FileLoader.load_data('resources/logs/log_ml_out_2.json')
    control = data['control_events']
    test = data['test_events']
    unknown = data['unknown_events']
    corpus = LogCorpus()
    for events in control.values():
        for event in events:
            corpus.add_event(event, 'control_prev')

    for events in test.values():
        for event in events:
            corpus.add_event(event, 'test_prev')

    sio = SplunkIntelOptimized(corpus, SplunkIntelOptimized.parse(['--sim_threshold', '0.9']))
    result = sio.run()
    result_dict = json.loads(result.get_output_for_notebook_as_json())
    expected_result = FileLoader.load_data('resources/logs/log_ml_out_2_expected_result.json')

    # make sure x, y are removed before comparison
    for key in ['control_clusters', 'test_clusters', 'unknown_clusters']:
        for val in result_dict[key].values():
            for sub_val in val.values():
                del sub_val['x']
                del sub_val['y']

    for key in ['control_clusters', 'test_clusters', 'unknown_clusters']:
        for val in expected_result[key].values():
            for sub_val in val.values():
                del sub_val['x']
                del sub_val['y']

    assert bool(expected_result['control_events'] == result_dict['control_events'])
    assert bool(expected_result['test_events'] == result_dict['test_events'])
    assert bool(expected_result['unknown_events'] == result_dict['unknown_events'])
    assert bool(expected_result['control_clusters'] == result_dict['control_clusters'])
    assert bool(expected_result['test_clusters'] == result_dict['test_clusters'])

    # anom cluster might have different labels
    matched_anom = 0
    for anom in result_dict['unknown_clusters'].values():
        # remove cluster lable, it is not the same each time
        del anom.values()[0]['cluster_label']
        for exp_anom in expected_result['unknown_clusters'].values():
            if 'cluster_label' in exp_anom.values()[0]:
                del exp_anom.values()[0]['cluster_label']
            if bool(anom==exp_anom):
                matched_anom +=1
                break
    assert matched_anom == 3





def main(args):
    test_log_ml_out_2()


if __name__ == "__main__":
    main(sys.argv)
