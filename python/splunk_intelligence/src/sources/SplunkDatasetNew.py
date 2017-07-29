import json
import logging
import sys

import numpy as np

from SplunkFileSource import SplunkFileSource
from SplunkHarnessLoader import SplunkHarnessLoader

logger = logging.getLogger(__name__)


class SplunkDatasetNew(object):
    def __init__(self):
        self.control_events = {}
        self.test_events = {}

        self.control_clusters = {}
        self.test_clusters = {}
        self.anomalies = []
        self.anom_clusters = {}
        self.new_data = True

    def add_event(self, event, event_type):

        if 'host' in event:
            host = event.get('host')
        else:
            host = 'Unknown'

        if event_type == 'control':
            if event['cluster_label'] not in self.control_events:
                self.control_events[event['cluster_label']] = []
            # print(event['cluster_label'], event, self.control_events[event['cluster_label']])
            self.control_events[event['cluster_label']].append(
                dict(cluster_label=int(event['cluster_label']), text=event.get('_raw'),
                     message_frequencies=[dict(count=event.get('cluster_count'), time=event.get('_time'), host=host,
                                               old_label=event['cluster_label'])]))
        elif event_type == 'test':
            if event['cluster_label'] not in self.test_events:
                self.test_events[event['cluster_label']] = []
            self.test_events[event['cluster_label']].append(
                dict(cluster_label=int(event['cluster_label']), text=event.get('_raw'),
                     message_frequencies=[dict(count=event.get('cluster_count'), time=event.get('_time'), host=host,
                                               old_label=event['cluster_label'])]))
        elif event_type == 'control_prod':
            if event['clusterLabel'] not in self.control_events:
                self.control_events[event['clusterLabel']] = []
            # print(event['cluster_label'], event, self.control_events[event['cluster_label']])
            self.control_events[event['clusterLabel']].append(
                dict(cluster_label=int(event['clusterLabel']), text=event.get('logMessage'),
                     message_frequencies=[dict(count=event.get('count'), time=event.get('timeStamp'),
                                               host=host,
                                               old_label=event.get('clusterLabel'))]))
        elif event_type == 'test_prod':
            if event['clusterLabel'] not in self.test_events:
                self.test_events[event['clusterLabel']] = []
            # print(event['cluster_label'], event, self.control_events[event['cluster_label']])
            self.test_events[event['clusterLabel']].append(
                dict(cluster_label=int(event['clusterLabel']), text=event.get('logMessage'),
                     message_frequencies=[dict(count=event.get('count'),
                                               time=event.get('timeStamp'),
                                               host=host,
                                               old_label=event.get('clusterLabel'))]))

        elif event_type == 'control_prev':
            label = 10000 + event['cluster_label']
            if label not in self.control_events:
                self.control_events[label] = []
            self.control_events[label].append(
                dict(cluster_label=event['cluster_label'], text=event.get('text'), message_frequencies=event.get('message_frequencies')))
        elif event_type == 'test_prev':
            label = 10000 + event['cluster_label']
            if label not in self.test_events:
                self.test_events[label] = []
            # event.get('count').append(label)
            self.test_events[label].append(
                dict(cluster_label=event['cluster_label'], text=event.get('text'), message_frequencies=event.get('message_frequencies')))

    def save_to_harness(self, url, auth_token, payload):
        SplunkHarnessLoader.post_to_wings_server(url, auth_token, payload)

    # Called with the production workflow
    def load_from_harness(self, options):

        control_events = SplunkHarnessLoader.load_from_wings_server(options.control_input_url,
                                                                    options.auth_token,
                                                                    options.application_id,
                                                                    options.workflow_id,
                                                                    options.state_execution_id,
                                                                    options.log_collection_minute,
                                                                    options.control_nodes,
                                                                    options.query)

        test_events = SplunkHarnessLoader.load_from_wings_server(options.test_input_url,
                                                                 options.auth_token,
                                                                 options.application_id,
                                                                 options.workflow_id,
                                                                 options.state_execution_id,
                                                                 options.log_collection_minute,
                                                                 options.test_nodes,
                                                                 options.query)

        if control_events is None and test_events is None:
            logger.error("No new control events or test events")
            sys.exit(2)

        self.new_data = True
        for event in control_events:
            self.add_event(event, 'control')
        for event in test_events:
            self.add_event(event, 'test')

        prev_state = SplunkHarnessLoader.load_prev_output_from_harness(options.log_analysis_get_url,
                                                                       options.auth_token,
                                                                       options.application_id,
                                                                       options.state_execution_id,
                                                                       options.query)
        if prev_state is not None:

            if prev_state.get('control_events') is not None:
                for key, events in prev_state.get('control_events').items():
                    for event in events:
                        self.add_event(event, 'control_prev')

            if prev_state.get('test_events') is not None:
                for key, events in prev_state.get('test_events').items():
                    for event in events:
                        self.add_event(event, 'test_prev')

    # Used in SplunkAnomalyLegacy: legacy files that are not grouped by host
    def load_legacy_file(self, file_name, control_window, test_window, prev_out_file=None):
        raw_events = SplunkFileSource.load_data(file_name)
        minute = 0
        count = 0
        self.new_data = True
        for idx, event in enumerate(raw_events):
            count = count + 1
            if event.get('cluster_label') == '1':
                minute = minute + 1

            if control_window[0] <= minute <= control_window[1]:
                self.add_event(event, 'control')
            if test_window[0] <= minute <= test_window[1]:
                self.add_event(event, 'test')

        if not bool(self.control_events) and not bool(self.test_events):
            self.new_data = False
            return

        if prev_out_file is not None:
            prev_out = SplunkFileSource.load_data(prev_out_file)
            for key, events in prev_out.get('control_events').items():
                for event in events:
                    self.add_event(event, 'control_prev')

            for key, events in prev_out.get('test_events').items():
                for event in events:
                    self.add_event(event, 'test_prev')

    # Used in SplunkAnomaly : To debug prod runs
    def load_prod_file(self, file_name, control_window, test_window,
                       control_nodes, test_nodes, prev_out_file=None):
        raw_events = SplunkFileSource.load_data(file_name)
        print(control_window, test_window, control_nodes, test_nodes)
        self.new_data = True
        for idx, event in enumerate(raw_events):

            minute = event.get('logCollectionMinute')
            if control_window[0] <= minute <= control_window[1] \
                    and event.get('host') in control_nodes:
                self.add_event(event, 'control_prod')
            if test_window[0] <= minute <= test_window[1] \
                    and event.get('host') in test_nodes:
                self.add_event(event, 'test_prod')

        if not bool(self.control_events) and not bool(self.test_events):
            self.new_data = False
            return

        if prev_out_file is not None:
            prev_out = SplunkFileSource.load_data(prev_out_file)
            for key, events in prev_out.get('control_events').items():
                for event in events:
                    self.add_event(event, 'control_prev')

            for key, events in prev_out.get('test_events').items():
                for event in events:
                    self.add_event(event, 'test_prev')

    def get_control_events(self):
        return self.control_events

    def get_control_events_text_as_np(self):
        texts = []
        print('control count = ', len(self.control_events))
        for key, value in self.control_events.items():
            texts.append(value[0].get('text'))
        return np.array(texts)

    def get_test_events_text_as_np(self):
        texts = []
        print('test count = ', len(self.test_events))
        for key, value in self.test_events.items():
            texts.append(value[0].get('text'))
        return np.array(texts)

    def get_all_events_text_as_np(self):
        texts = []
        for key, value in self.control_events.items():
            texts.append(value[0].get('text'))
        for key, value in self.test_events.items():
            texts.append(value[0].get('text'))

        return np.array(texts)

    def get_control_clusters(self):
        return self.control_clusters

    def get_test_clusters(self):
        return self.test_clusters

    def get_unknown_anomalies_text(self):
        texts = []
        for anomal in self.anomalies:
            texts.append(anomal[0].get('text'))
        return texts

    def get_unknown_anomalies(self):
        return self.anomalies

    def get_cluster_tags(self, cluster_id, centroids, feature_names, max_terms):
        tags = []
        for i in centroids[cluster_id, :max_terms]:
            tags.append(feature_names[i])

        return tags

    def create_anom_clusters(self, clusters):
        for index, anomal in enumerate(self.anomalies):
            if clusters[index] not in self.anom_clusters:
                self.anom_clusters[clusters[index]] = {}
            for anom in anomal:
                host = anom.get('message_frequencies')[0].get('host')
                if host not in self.anom_clusters[clusters[index]]:
                    self.anom_clusters[clusters[index]][host] = dict(x=anom.get('x'),
                                                                     y=anom.get('y'),
                                                                     text=anom.get('text'),
                                                                     cluster_label=anom.get('cluster_label'),
                                                                     message_frequencies=[])

                self.anom_clusters[clusters[index]][host].get('message_frequencies').extend(
                    anom.get('message_frequencies'))

    def get_anom_clusters(self):
        return self.anom_clusters

    def create_clusters(self, combined_dist, clusters, centroids,
                        feature_names,
                        predictions, anomalies):
        for index, (key, value) in enumerate(self.control_events.items()):
            for val in value:
                val['cluster_label'] = clusters[index]
                if val.get('cluster_label') not in self.control_clusters:
                    self.control_clusters[val.get('cluster_label')] = {}

                host = val.get('message_frequencies')[0].get('host')
                if host not in self.control_clusters[val.get('cluster_label')]:
                    self.control_clusters[val.get('cluster_label')][host] = dict(x=combined_dist[index, 0],
                                                                                 y=combined_dist[index, 1],
                                                                                 text=val.get('text'),
                                                                                 cluster_label=val.get('cluster_label'),
                                                                                 message_frequencies=[],
                                                                                 tags=self.get_cluster_tags(
                                                                                     val.get('cluster_label'),
                                                                                     centroids,
                                                                                     feature_names,
                                                                                     5))
                self.control_clusters[val.get('cluster_label')][host].get('message_frequencies').extend(
                    val.get('message_frequencies'))

        dist_offset = len(self.control_events)
        anomaly_index = 1000000

        for index, (key, value) in enumerate(self.test_events.items()):
            anomal = []
            for val in value:
                if anomalies[index] == 1:
                    val['cluster_label'] = predictions[index]
                else:
                    val['cluster_label'] = anomaly_index
                    anomaly_index = anomaly_index + 1

                # TODO make this a class constant
                if val.get('cluster_label') < 1000000:
                    if val.get('cluster_label') not in self.test_clusters:
                        self.test_clusters[val.get('cluster_label')] = {}

                    host = val.get('message_frequencies')[0].get('host')
                    if host not in self.test_clusters[val.get('cluster_label')]:
                        self.test_clusters[val.get('cluster_label')][host] = dict(
                            x=combined_dist[index + dist_offset, 0],
                            y=combined_dist[index + dist_offset, 1],
                            text=val.get('text'),
                            cluster_label=val.get('cluster_label'),
                            message_frequencies=[],
                            anomalous_counts=[],
                            unexpected_freq=False,
                            tags=self.get_cluster_tags(
                                val.get('cluster_label'),
                                centroids,
                                feature_names,
                                5))

                    self.test_clusters[val.get('cluster_label')][host].get('message_frequencies').extend(
                        val.get('message_frequencies'))

                else:
                    anomal.append(dict(x=combined_dist[index + dist_offset, 0],
                                       y=combined_dist[index + dist_offset, 1],
                                       text=val.get('text'),
                                       cluster_label=predictions[index],
                                       message_frequencies=val.get('message_frequencies')))
            if len(anomal) > 0:
                self.anomalies.append(anomal)

    def get_output_as_json(self, options):

        return json.dumps(dict(query=options.query, control_events=self.control_events, test_events=self.test_events,
                               unknown_events=self.anomalies,
                               control_clusters=self.control_clusters, test_clusters=self.test_clusters,
                               unknown_clusters=self.anom_clusters
                               ))

    @property
    def get_output_for_notebook_as_json(self):

        return json.dumps(dict(control_events=self.control_events, test_events=self.test_events,
                               unknown_events=self.anomalies,
                               control_clusters=self.control_clusters, test_clusters=self.test_clusters,
                               unknown_clusters=self.anom_clusters
                               ))

    def control_scatter_plot(self):

        x = []
        y = []
        labels = []
        tooltips = []
        sizes = []
        ind = 0
        for index, (key, value) in enumerate(self.control_clusters.items()):
            for host, val in value.items():
                x.append(val.get('x'))
                y.append(val.get('y'))
                tooltips.append([host + '<br>' + str(val.get('cluster_label')) + '<br>' + val.get('text'), ind])
                ind = ind + 1
                labels.append(0)
                size = 0
                for freq in val.get('message_frequencies'):
                    size = size + int(freq.get('count'))
                    sizes.append(size)

        for index, (key, value) in enumerate(self.test_clusters.items()):
            for host, val in value.items():
                x.append(val.get('x'))
                y.append(val.get('y'))
                tooltips.append([host + '<br>' + str(val.get('cluster_label')) + '<br>' + val.get('text'), ind])
                ind = ind + 1
                if val.get('unexpected_freq'):
                    labels.append(3)
                else:
                    labels.append(1)
                # labels.append(1)
                size = 0
                for freq in val.get('message_frequencies'):
                    size = size + int(freq.get('count'))
                    sizes.append(size)

        for key, value in self.anom_clusters.items():
            for host, val in value.items():
                x.append(val.get('x'))
                y.append(val.get('y'))
                tooltips.append([host + '<br>' + str(val.get('cluster_label')) + '<br>' + val.get('text'), ind])
                ind = ind + 1
                labels.append(2)
                size = 0
                for freq in val.get('message_frequencies'):
                    size = size + int(freq.get('count'))
                    sizes.append(size)

        return np.column_stack((x, y)), tooltips, labels, sizes

    def count_hist_plot(self):

        data = {}
        for index, (key, value) in enumerate(self.control_clusters.items()):
            for host, val in value.items():
                for freq in val.get('message_frequencies'):
                    if key not in data:
                        data[key] = {}
                        data[key]['control'] = []
                        data[key]['test'] = []
                    data[key]['control'].append(freq.get('count'))

        for index, (key, value) in enumerate(self.test_clusters.items()):
            for host, val in value.items():
                for freq in val.get('message_frequencies'):
                    data[key]['test'].append(freq.get('count'))

        return data

    def count_scatter_plot(self):

        x = []
        y = []
        labels = []
        tooltips = []
        ind = 0
        sizes = []
        for index, (key, value) in enumerate(self.test_clusters.items()):
            for host, val in value.items():
                x.append(val.get('x'))
                y.append(val.get('y'))
                tooltips.append([host + '<br>' + str(val.get('cluster_label')) + '<br>' + val.get('text'), ind])
                ind = ind + 1
                size = 0
                for freq in val.get('message_frequencies'):
                    size = size + int(freq.get('count'))
                    sizes.append(size)
                if val.get('unexpected_freq'):
                    labels.append(1)
                else:
                    labels.append(0)

        return np.column_stack((x, y)), tooltips, labels, sizes

    def control_scatter_plot_4d(self):

        x = []
        y = []
        z = []
        labels = []
        tooltips = []
        ind = 0
        clusters = []
        for index, (key, value) in enumerate(self.control_clusters.items()):
            for host, val in value.items():
                for freq in val.get('message_frequencies'):
                    x.append(val.get('x'))
                    y.append(val.get('y'))
                    z.append(freq.get('count'))
                    tooltips.append([host + '<br>' + self.split(val.get('text'), len(val.get('text')), 30), ind])
                    labels.append(0)
                    clusters.append(key)
                ind = ind + 1

        for index, (key, value) in enumerate(self.test_clusters.items()):
            for host, val in value.items():
                for idx, freq in enumerate(val.get('message_frequencies')):
                    x.append(val.get('x'))
                    y.append(val.get('y'))
                    z.append(freq.get('count'))
                    tooltips.append([host + '<br>' + self.split(val.get('text'), len(val.get('text')), 30), ind])
                    if val.get('anomalous_counts')[idx] == 1:
                        labels.append(1)
                    else:
                        labels.append(2)
                    clusters.append(key)
                ind = ind + 1

        return np.column_stack((x, y, z)), tooltips, labels, clusters

    def get_all_events_for_notebook(self):
        texts = []
        for index, (key, value) in enumerate(self.control_clusters.items()):
            for host, val in value.items():
                texts.append(val)

        for index, (key, value) in enumerate(self.test_clusters.items()):
            for host, val in value.items():
                texts.append(val)

        for anomal in self.anomalies:
            for val in anomal:
                texts.append(val)

        return texts

    def split(self, input, length, size):
        input.replace('\n', ' ')
        input.replace('\tat', ' ')
        return '<br>'.join([input[start:start + size] for start in range(0, length, size)])
