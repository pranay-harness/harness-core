from core.util.TimeSeriesUtils import MetricToDeviationType, MetricType


class MetricTemplate(object):
    def __init__(self, metric_template):
        self.metric_template = metric_template

    def get_metric_names(self):
        metric_names = []
        for metrics in self.metric_template.values():
            metric_names.append(metrics['metricName'])
        return metric_names

    def get_metric_names_by_tags(self):
        metric_names = {'default':[]}
        for metrics in self.metric_template.values():
            if 'tags' in metrics and metrics.get('tags') is not None:
                if len(metrics.get('tags')) != 0:
                    for tag in metrics['tags']:
                        if not tag in metric_names:
                            metric_names[tag] = []
                        metric_names[tag].append(metrics['metricName'])
            metric_names['default'].append(metrics['metricName'])
        return metric_names

    def get_deviation_type(self, metric_name):
        threshold_type = self.metric_template[metric_name]['thresholds'][0]['thresholdType']
        if threshold_type == 'ALERT_WHEN_HIGHER':
            return MetricToDeviationType.HIGHER
        elif threshold_type == 'ALERT_WHEN_LOWER':
            return MetricToDeviationType.LOWER
        elif threshold_type == 'ALERT_HIGHER_OR_LOWER':
            return MetricToDeviationType.BOTH
        else:
            raise ValueError('Unknown alert type ' + threshold_type)

    def get_deviation_min_threshold(self, metric_name, threshold_comparison_type):
        for threshold in self.metric_template[metric_name]['thresholds']:
            if threshold['comparisonType'] == threshold_comparison_type.value:
                return threshold['min']
        raise ValueError('Cannot find threshold_comparison_type ' + threshold_comparison_type.value
                         + ' for metric ' + metric_name)

    def get_metric_type(self, metric_name):
        return MetricType[self.metric_template[metric_name]['metricType']]

    def get_metric_name(self, metric_type):
        for metric in self.metric_template.values():
            if metric['metricType'] == metric_type.value:
                return metric['metricName']

        return None
