import numpy as np
import pandas as pd

from sources.SplunkFileSource import SplunkFileSource


class SplunkDataset(object):
    raw_events = []
    all_events = []
    control_events = []
    test_events = []

    control_clusters = []
    test_clusters = []

    anomalies = []
    anomolous_values = []

    xy_matrix_control = []
    xy_matrix_all = []

    centroids = []
    feature_names = []

    def load_from_file(self, file_name, control_window, test_window):
        self.raw_events = SplunkFileSource.load_data(file_name)
        minute = 0
        for id, dict in enumerate(self.raw_events):
            if dict.get('cluster_label') == '1':
                minute = minute + 1
            if control_window[0] <= minute <= control_window[1]:
                self.all_events.append([dict.get('_raw'), len(self.control_events) - 1, dict.get('cluster_count')])
                self.control_events.append(
                    [dict.get('_raw'), len(self.control_events) - 1, dict.get('cluster_count'),
                     len(self.all_events) - 1])
            if test_window[0] <= minute <= test_window[1]:
                self.all_events.append([dict.get('_raw'), len(self.test_events) - 1, dict.get('cluster_count')])
                self.test_events.append([dict.get('_raw'), len(self.test_events) - 1, dict.get('cluster_count'),
                                         len(self.all_events) - 1])

        self.anomolous_values = [-1] * len(self.test_events)

    def get_all_events(self):
        return self.all_events

    def get_all_events_text(self):
        return [event[0] for event in self.all_events]

    def get_all_events_as_np(self):
        return np.array(self.all_events)[:, 0]

    def get_control_events(self):
        return self.control_events

    def get_control_events_as_np(self):
        return np.array(self.control_events)[:, 0]

    def get_test_events(self):
        return self.test_events

    def get_test_events_as_np(self):
        return np.array(self.test_events)[:, 0]

    def set_control_clusters(self, clusters):
        self.control_clusters = clusters

    def set_test_clusters(self, clusters):
        self.test_clusters = clusters

    def set_anomalies(self, anomalies):
        self.anomalies = anomalies

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
                                 label=control_values[:, 0], text=control_values[:, 1], idx=control_values[:, 2]))

    def get_test_values_pd(self):
        test_values = np.column_stack((self.test_clusters, np.array(self.test_events)))[self.anomalies != -1]
        return pd.DataFrame(dict(x=test_values[:, 0], y=test_values[:, 3],
                                 label=test_values[:, 0], text=test_values[:, 1], idx=test_values[:, 2]))

    def set_anomalous_values(self, id_list, values):
        for i, id in enumerate(id_list):
            self.anomolous_values[int(id)] = values[i]

    def get_control_clusters(self):
        return self.control_clusters

    def get_test_clusters(self):
        return self.test_clusters

    def get_all_clusters(self):
        all_clusters = list(self.control_clusters)
        all_clusters.extend(list(self.test_clusters))
        return all_clusters

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
        return np.column_stack((self.get_xy_matrix_all(), np.array(self.all_events)[:, 2]))

    def get_anomalous_values(self):
        return self.anomolous_values
