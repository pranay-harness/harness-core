# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import sys
from LogNeuralNet import LogNeuralNet
from SplunkIntelOptimized import SplunkIntelOptimized
from sources.FileLoader import FileLoader
from sources.LogCorpus import LogCorpus


def get_data(mode):
    data_file = "resources/logs/user_feedback_all_events.json"
    ignore_file ="resources/logs/user_feedback_ignore_events.json"
    data = FileLoader.load_data(data_file)
    corpus = LogCorpus()
    #add base event to control and test
    corpus.add_event(data['base_events'], 'control_prev')
    corpus.add_event(data['base_events'], 'test_prev')
    if mode == 'ignore_in_control':
        # add event like ignore to control
        corpus.add_event(data['like_ignore_events'], 'control_prev')
    elif mode == 'ignore_in_test':
        # add event like ignore to test
        corpus.add_event(data['like_ignore_events'], 'test_prev')
        # add anom to test
        corpus.add_event(data['anom_events'], 'test_prev')
    # loading ignore and adding to control events
    ignore_event = FileLoader.load_data(ignore_file)
    ignore_event['cluster_label'] = 0
    corpus.add_event(ignore_event, 'user_feedback')
    return corpus


def test_splunk_ignore_in_control():
    corpus = get_data(mode='ignore_in_control')
    sp_cluster = SplunkIntelOptimized(corpus, SplunkIntelOptimized.parse(['--sim_threshold', '0.9']))
    result = sp_cluster.run()
    assert len(result.anom_clusters) == 0
    assert len(result.control_clusters) == 1
    assert len(result.test_clusters) == 1
    assert len(result.ignore_clusters) == 1
    assert len(result.control_events) == 3

def test_splunk_ignore_in_test_with_anomaly():
    corpus = get_data(mode='ignore_in_test')
    sp_cluster = SplunkIntelOptimized(corpus, SplunkIntelOptimized.parse(['--sim_threshold', '0.9']))
    result = sp_cluster.run()
    assert len(result.test_events) == 3
    assert len(result.anom_clusters) == 1
    assert len(result.control_clusters) == 1
    assert len(result.test_clusters) == 1
    assert len(result.ignore_clusters) == 1

def test_nn_ignore_in_control():
    corpus = get_data(mode='ignore_in_control')
    dv_cluster = LogNeuralNet(corpus, LogNeuralNet.parse(['--sim_threshold', '0.96']))
    result = dv_cluster.run()
    assert len(result.anom_clusters) == 0
    assert len(result.control_clusters) == 1
    assert len(result.test_clusters) == 1
    assert len(result.ignore_clusters) == 1
    assert len(result.control_events) == 3

def test_nn_ignore_in_test_with_anomaly():
    corpus = get_data(mode='ignore_in_test')
    dv_cluster = LogNeuralNet(corpus, LogNeuralNet.parse(['--sim_threshold', '0.96']))
    result = dv_cluster.run()
    assert len(result.test_events) == 3
    assert len(result.anom_clusters) == 1
    assert len(result.control_clusters) == 1
    assert len(result.test_clusters) == 1
    assert len(result.ignore_clusters) == 1

def main():
    test_splunk_ignore_in_control()
    test_splunk_ignore_in_test_with_anomaly()
    test_nn_ignore_in_control()
    test_nn_ignore_in_test_with_anomaly()


if __name__ == "__main__":
    main(sys.argv)
