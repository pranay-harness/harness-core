import sys

from core.util.TimeSeriesUtils import MetricToDeviationType, ThresholdComparisonType, MetricType
from sources.FileLoader import FileLoader
from sources.MetricTemplate import MetricTemplate


def test_load_metric_template():
    metric_template = MetricTemplate(FileLoader.load_data('resources/ts/metric_template.json'))
    met_names = set([u'requestsPerMinute', u'averageResponseTime', u'apdexScore', u'error'])
    assert len(met_names & set(metric_template.get_metric_names())) == 4
    assert metric_template.get_deviation_type('averageResponseTime') == MetricToDeviationType.HIGHER
    assert metric_template.get_deviation_type('requestsPerMinute') == MetricToDeviationType.LOWER
    assert metric_template.get_deviation_type('apdexScore') == MetricToDeviationType.LOWER
    assert metric_template.get_deviation_type('error') == MetricToDeviationType.HIGHER

    assert metric_template.get_metric_type('averageResponseTime') == MetricType.RESP_TIME
    assert metric_template.get_metric_type('requestsPerMinute') == MetricType.THROUGHPUT
    assert metric_template.get_metric_type('apdexScore') == MetricType.VALUE
    assert metric_template.get_metric_type('error') == MetricType.ERROR

    assert metric_template.get_deviation_min_threshold('averageResponseTime', ThresholdComparisonType.DELTA) == 50
    assert metric_template.get_deviation_min_threshold('requestsPerMinute', ThresholdComparisonType.DELTA) == 20
    assert metric_template.get_deviation_min_threshold('apdexScore', ThresholdComparisonType.DELTA) == 0.3
    assert metric_template.get_deviation_min_threshold('error', ThresholdComparisonType.DELTA) == 0

    assert metric_template.get_deviation_min_threshold('averageResponseTime', ThresholdComparisonType.RATIO) == 0.5
    assert metric_template.get_deviation_min_threshold('requestsPerMinute', ThresholdComparisonType.RATIO) == 0.5
    assert metric_template.get_deviation_min_threshold('apdexScore', ThresholdComparisonType.RATIO) == 0.5
    assert metric_template.get_deviation_min_threshold('error', ThresholdComparisonType.RATIO) == 0.5

    assert metric_template.get_metric_name(MetricType.THROUGHPUT) == 'requestsPerMinute'


def main(args):
    test_load_metric_template()

if __name__ == "__main__":
    main(sys.argv)