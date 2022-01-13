# Copyright 2018 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

from core.util.TimeSeriesUtils import MetricToDeviationType, MetricType, ThresholdComparisonType


class MetricTemplate(object):
    def __init__(self, metric_template):
        self.metric_template = metric_template

    def get_metric_names(self):
        metric_names = []
        for metrics in self.metric_template['default'].values():
            metric_names.append(metrics['metricName'])
        return metric_names

    def get_metric_names_by_tags(self):
        metric_names = {'default':[]}
        for metrics in self.metric_template['default'].values():
            if 'tags' in metrics and metrics.get('tags') is not None:
                if len(metrics.get('tags')) != 0:
                    for tag in metrics['tags']:
                        if not tag in metric_names:
                            metric_names[tag] = []
                        metric_names[tag].append(metrics['metricName'])
            metric_names['default'].append(metrics['metricName'])
        return metric_names

    def get_deviation_type(self, metric_name):
        for threshold in self.metric_template['default'][metric_name]['thresholds']:
            if threshold['comparisonType'] == 'RATIO':
                threshold_type = threshold['thresholdType']
                if threshold_type == 'ALERT_WHEN_HIGHER':
                    return MetricToDeviationType.HIGHER
                elif threshold_type == 'ALERT_WHEN_LOWER':
                    return MetricToDeviationType.LOWER
                elif threshold_type == 'ALERT_HIGHER_OR_LOWER':
                    return MetricToDeviationType.BOTH
                else:
                    raise ValueError('Unknown alert type ' + threshold_type)
        raise ValueError('No threshold found for RATIO for metric name ' + metric_name)

    def get_deviation_threshold(self, metric_name, threshold_comparison_type):
        for threshold in self.metric_template['default'][metric_name]['thresholds']:
            if threshold['comparisonType'] == threshold_comparison_type.value:
                return threshold['ml']
        raise ValueError('Cannot find threshold_comparison_type ' + threshold_comparison_type.value
                         + ' for metric ' + metric_name)

    def get_abs_deviation_range(self, transaction_name, metric_name):
        min_threshold = float('inf')
        max_threshold = float('-inf')
        if transaction_name in self.metric_template and metric_name in self.metric_template[transaction_name]:
            for threshold in self.metric_template[transaction_name][metric_name]['customThresholds']:
                if threshold['comparisonType'] == ThresholdComparisonType.ABSOLUTE.value:
                    if threshold['thresholdType'] == 'ALERT_WHEN_HIGHER':
                        max_threshold = threshold['ml']
                    else:
                        min_threshold = threshold['ml']
            return [min_threshold,max_threshold]
        for threshold in self.metric_template['default'][metric_name]['thresholds']:
            if threshold['comparisonType'] == ThresholdComparisonType.ABSOLUTE.value:
                if threshold['thresholdType'] == 'ALERT_WHEN_HIGHER':
                    max_threshold = threshold['ml']
                else:
                    min_threshold = threshold['ml']
        return [min_threshold, max_threshold]

    def get_metric_type(self, metric_name):
        return MetricType[self.metric_template['default'][metric_name]['metricType']]

    #TODO - this is only used for throughput now. what if we have multiple
    #TODO - metrics of same type
    def get_metric_name(self, metric_type):
        for metric in self.metric_template['default'].values():
            if metric['metricType'] == metric_type.value:
                return metric['metricName']

        return None
