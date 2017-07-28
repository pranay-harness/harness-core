import json

import numpy as np
import pandas as pd

from SplunkFileSource import SplunkFileSource
from SplunkHarnessLoader import SplunkHarnessLoader


class SplunkDataset(object):
    raw_events = []
    all_events = []
    control_events = []
    test_events = []

    control_clusters = []
    test_clusters = []

    anomalies = []

    xy_matrix_control = []
    xy_matrix_all = []

    centroids = []
    feature_names = []

    def add_event(self, dict, type):

        if 'host' in dict:
            host = dict.get('host')
        else:
            host = None

        if type == 'control':
            self.all_events.append(
                pd.Series([dict.get('_raw'), len(self.all_events) - 1, dict.get('cluster_count'), -1, 0, 0,
                           dict.get('_time'), host],
                          index=['text', 'local_id', 'count', 'cluster_id', 'anomaly_label',
                                 'anomaly_count_label', 'time', 'host']))
            self.control_events.append(
                pd.Series([dict.get('_raw'), len(self.control_events) - 1, dict.get('cluster_count'),
                           len(self.all_events) - 1],
                          index=['text', 'local_id', 'count', 'global_id']))
        else:
            self.all_events.append(
                pd.Series(
                    [dict.get('_raw'), len(self.all_events) - 1, dict.get('cluster_count'), -1, 1, 1, dict.get('_time'),
                     host]
                    , index=['text', 'local_id', 'count', 'cluster_id', 'anomaly_label',
                             'anomaly_count_label', 'time', 'host']))
            self.test_events.append(
                pd.Series([dict.get('_raw'), len(self.test_events) - 1, dict.get('cluster_count'),
                           len(self.all_events) - 1],
                          index=['text', 'local_id', 'count', 'global_id']))

    def load_from_harness(self, options):
        control_events = SplunkHarnessLoader.load_from_wings_server(options.control_input_url,
                                                                    options.application_id,
                                                                    options.workflow_id,
                                                                    options.control_window[0],
                                                                    options.control_window[1],
                                                                    options.control_nodes,
                                                                    options.query)
        for dict in control_events:
            self.add_event(dict, 'control')

        test_events = SplunkHarnessLoader.load_from_wings_server(options.test_input_url,
                                                                 options.application_id,
                                                                 options.workflow_id,
                                                                 options.test_window[0],
                                                                 options.test_window[1],
                                                                 options.test_nodes,
                                                                 options.query)
        for dict in test_events:
            self.add_event(dict, 'test')

        self.raw_events.extend(control_events)
        self.raw_events.extend(test_events)

    # New files grouped by host
    def load_from_file_slice_by_nodes(self, file_name, test_nodes):
        self.raw_events = SplunkFileSource.load_data(file_name)
        for dict in self.raw_events:
            if dict.get('host') in test_nodes:
                self.add_event(dict, 'test')
            else:
                self.add_event(dict, 'control')

    # old files that are not grouped by host
    def load_from_file(self, file_name, control_window, test_window):
        self.raw_events = SplunkFileSource.load_data(file_name)
        minute = 0
        count = 0
        for id, dict in enumerate(self.raw_events):
            count = count + 1
            if dict.get('cluster_label') == '1':
                if minute > 0:
                    print(str(minute) + ' min : ' + str(count))

                minute = minute + 1

            if control_window[0] <= minute <= control_window[1]:
                self.add_event(dict, 'control')
            if test_window[0] <= minute <= test_window[1]:
                self.add_event(dict, 'test')

    def get_all_events(self):
        return self.all_events

    def get_all_events_text(self):
        return [event['text'] for event in self.all_events]

    def get_all_events_text_as_np(self):
        return np.array(self.get_all_events_text())

    def get_control_events(self):
        return self.control_events

    def get_control_events_text_as_np(self):
        return np.array([event['text'] for event in self.control_events])

    def get_test_events(self):
        return self.test_events

    def get_test_events_text_as_np(self):
        return np.array([event['text'] for event in self.test_events])

    def set_control_clusters(self, clusters):
        self.control_clusters = clusters
        for i, c in enumerate(self.control_clusters):
            self.all_events[self.control_events[i]['global_id']]['cluster_id'] = c

    def set_test_clusters(self, clusters):
        self.test_clusters = clusters
        for i, c in enumerate(self.test_clusters):
            self.all_events[self.test_events[i]['global_id']]['cluster_id'] = c

    def set_anomalies(self, anomalies):
        self.anomalies = anomalies
        for i, c in enumerate(self.anomalies):
            if c == -1:
                self.all_events[self.test_events[i]['global_id']]['cluster_id'] = c
                self.all_events[self.test_events[i]['global_id']]['anomaly_label'] = 2

    def set_xy_matrix_all(self, xy_matrix):
        self.xy_matrix_all = xy_matrix

    def get_xy_matrix_all(self):
        return self.xy_matrix_all

    def set_xy_matrix_control(self, xy_matrix):
        self.xy_matrix_control = xy_matrix

    def get_xy_matrix_control(self):
        return self.xy_matrix_control

    def get_control_values_pd(self):
        control_values = np.column_stack((self.control_clusters, np.array(self.control_events)))
        return pd.DataFrame(dict(x=control_values[:, 0], y=control_values[:, 3],
                                 label=control_values[:, 0], text=control_values[:, 1], idx=control_values[:, 4]))

    def get_test_values_pd(self):
        test_values = np.column_stack((self.test_clusters, np.array(self.test_events)))[self.anomalies != -1]
        return pd.DataFrame(dict(x=test_values[:, 0], y=test_values[:, 3],
                                 label=test_values[:, 0], text=test_values[:, 1], idx=test_values[:, 4]))

    def set_anomalous_values(self, id_list, values):
        for i, id in enumerate(id_list):
            if values[i] == -1:
                self.all_events[int(id)]['anomaly_count_label'] = 2

    def get_control_clusters(self):
        return self.control_clusters

    def get_test_clusters(self):
        return self.test_clusters

    def get_all_clusters(self):
        return [event['cluster_id'] for event in self.all_events]

    def set_centroids(self, centroids):
        self.centroids = centroids

    def get_centroids(self):
        return self.centroids

    def set_feature_names(self, feature_names):
        self.feature_names = feature_names

    def get_feature_names(self):
        return self.feature_names

    def get_cluster_tags(self, max_terms):
        tags = []
        for i in range(len(set(self.control_clusters))):
            tag = 'cluster = ' + str(i) + '<br>'
            for j in self.centroids[i, :max_terms]:
                tag = tag + self.feature_names[j] + '<br>'
            # tags.append(self.split(tag, min(500, len(tag)), 100))
            tags.append(tag)
        return tags

    def get_anomalies(self):
        return self.anomalies

    def get_xyz_matrix_all(self):
        return np.column_stack((self.get_xy_matrix_all(), np.array([event['count'] for event in self.all_events])))

    def get_anomalous_values(self):
        return self.anomolous_values

    def get_raw_events(self):
        return self.raw_events

    def get_all_events_as_json(self):
        result = []
        clusters = {}
        for idx, event in enumerate(self.get_all_events()):
            if event['cluster_id'] not in clusters:
                clusters[event['cluster_id']] = dict(x=self.get_xyz_matrix_all()[idx, 0],
                                                     y=self.get_xyz_matrix_all()[idx, 1],
                                                     z=self.get_xyz_matrix_all()[idx, 2])

            result.append(dict(logMessage=event['text'], timestamp=event['time'], count=event['count'],
                               cluster_id=event['cluster_id'], anomalyLabel=event['anomaly_label'],
                               anomalyCountLabel=event['anomaly_count_label'],
                               host=event['host'],
                               x=clusters[event['cluster_id']].get('x'),
                               y=clusters[event['cluster_id']].get('y'),
                               z=clusters[event['cluster_id']].get('z')))
        return json.dumps(result)
