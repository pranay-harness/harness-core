import sys

from SplunkIntelOptimized import SplunkIntelOptimized
from sources.LogCorpus import LogCorpus
from sources.FileLoader import FileLoader


def test_ignore_in_control():
    print('testing user feedback with an ignore in control')
    data_file = "resources/logs/user_feedback_all_events.json"
    ignore_file ="resources/logs/user_feedback_ignore_events.json"
    data = FileLoader.load_data(data_file)
    corpus = LogCorpus()
    #add base event to control and test
    corpus.add_event(data['base_events'], 'control_prev')
    corpus.add_event(data['base_events'], 'test_prev')
    # add event like ignore to control
    corpus.add_event(data['like_ignore_events'], 'control_prev')
    # loading ignore and adding to control events
    ignore_event = FileLoader.load_data(ignore_file)
    ignore_event['cluster_label'] = 0
    corpus.add_event(ignore_event, 'user_feedback')
    sp_cluster = SplunkIntelOptimized(corpus, SplunkIntelOptimized.parse(['--sim_threshold', '0.9']))
    result = sp_cluster.run()
    assert len(result.anom_clusters) == 0
    assert len(result.control_clusters) == 1
    assert len(result.test_clusters) == 1
    assert len(result.ignore_clusters) == 1
    assert len(result.control_events) == 3

def test_ignore_in_test_with_anomaly():
    print('testing user feedback with an ignore and anomaly in test')
    data_file = "resources/logs/user_feedback_all_events.json"
    ignore_file ="resources/logs/user_feedback_ignore_events.json"
    data = FileLoader.load_data(data_file)
    corpus = LogCorpus()
    #add base event to control and test
    corpus.add_event(data['base_events'], 'control_prev')
    corpus.add_event(data['base_events'], 'test_prev')
    # add event like ignore to test
    corpus.add_event(data['like_ignore_events'], 'test_prev')
    # add anom to test
    corpus.add_event(data['anom_events'], 'test_prev')
    # loading ignore and adding to control events
    ignore_event = FileLoader.load_data(ignore_file)
    ignore_event['cluster_label'] = 0
    corpus.add_event(ignore_event, 'user_feedback')
    sp_cluster = SplunkIntelOptimized(corpus, SplunkIntelOptimized.parse(['--sim_threshold', '0.9']))
    result = sp_cluster.run()
    assert len(result.test_events) == 3
    assert len(result.anom_clusters) == 1
    assert len(result.control_clusters) == 1
    assert len(result.test_clusters) == 1
    assert len(result.ignore_clusters) == 1


def main(args):
    test_ignore_in_control()
    test_ignore_in_test_with_anomaly()


if __name__ == "__main__":
    main(sys.argv)