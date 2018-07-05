import argparse
import json
import sys

from LogNeuralNet import LogNeuralNet
from sources.LogCorpus import LogCorpus
from sources.FileLoader import FileLoader

options = LogNeuralNet.parse(['--sim_threshold=0.96'])


#test it removes False positive
def test_remove_fp():
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

        splunk_intel = LogNeuralNet(corpus, options)
        corpus = splunk_intel.run()
        assert len(corpus.anom_clusters) == 0

        control_start = control_start + 1
        test_start = test_start + 1


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

    sio = LogNeuralNet(corpus, LogNeuralNet.parse(['--sim_threshold', '0.96']))
    result = sio.run()
    result_dict = json.loads(result.get_output_for_notebook_as_json())
    expected_result = FileLoader.load_data('resources/logs/log_ml_out_2_expected_result.json')

    # make sure x, y are removed before comparison
    for key in ['control_clusters', 'test_clusters', 'unknown_clusters']:
        for val in result_dict[key].values():
            for sub_val in val.values():
                del sub_val['x']
                del sub_val['y']
                if key in ('control_clusters', 'test_clusters'):
                    del sub_val['tags']

    for key in ['control_clusters', 'test_clusters', 'unknown_clusters']:
        for val in expected_result[key].values():
            for sub_val in val.values():
                del sub_val['x']
                del sub_val['y']
                if key in ('control_clusters', 'test_clusters'):
                    del sub_val['tags']

    assert bool(expected_result['control_clusters'] == result_dict['control_clusters'])
    assert bool(expected_result['test_clusters'] == result_dict['test_clusters'])

    # anom cluster might have different labels
    matched_anom = 0
    for anom in result_dict['unknown_clusters'].values():
        for exp_anom in expected_result['unknown_clusters'].values():
            if bool(exp_anom.values()[0]['text'] == anom.values()[0]['text']):
                matched_anom +=1
                break
    assert matched_anom == 3

def main(args):
    test_log_ml_out_2()
    test_remove_fp()

if __name__ == "__main__":
    main(sys.argv)
