import numpy as np
from enum import Enum
from math import ceil


class RiskLevel(Enum):
    NA = -1


class ThresholdComparisonType(Enum):
    DELTA = 'DELTA'
    RATIO = 'RATIO'
    ABSOLUTE = 'ABSOLUTE'


class MetricType(Enum):
    # Metrics that represents any observation
    VALUE = 'VALUE'

    # Metrics that measure time
    RESP_TIME = 'RESP_TIME'

    # Metrics that count invocations
    THROUGHPUT = 'THROUGHPUT'

    # Metrics that count error
    ERROR = 'ERROR'

    # Metrics that count something
    COUNT = 'COUNT'

    # All Infra metrics
    INFRA = 'INFRA'


class MetricToDeviationType(Enum):
    HIGHER = 0
    LOWER = 1
    BOTH = 2


def get_deviation_type(metric_name):
    if 'averageResponseTime' == metric_name or \
                    'error' == metric_name:
        type = MetricToDeviationType.HIGHER
    elif 'throughput' == metric_name or \
                    'apdexScore' == metric_name or \
                    'requestsPerMinute' == metric_name:
        type = MetricToDeviationType.LOWER
    else:
        type = MetricToDeviationType.BOTH

    return type


def get_deviation_min_threshold(metric_name):
    if 'averageResponseTime' == metric_name:
        return 50
    elif 'throughput' == metric_name:
        return 0.3
    elif 'apdexScore' == metric_name:
        return 0.3
    elif 'requestsPerMinute' == metric_name:
        return 20
    elif 'other' == metric_name:
        return 0


def normalize_metric(control_data, test_data):
    """
    In place scaling of control and test values for a metric.

    :param control_data:  2d array_like observations for the control group
                            For a 2 node control group it will look something like below
                            [[150, 134, 125] , [123, 345, 432]
    :param test_data:  same as control values but for test group

    """

    # TODO use hellengers distance to compare the control and test distribution
    data = np.concatenate((control_data.flatten(), test_data.flatten()))
    mean = np.nanmean(data)
    std = np.nanstd(data)
    if std != 0.0 and std / mean > 0.01:
        control_data[np.isfinite(control_data)] = \
            (control_data[np.isfinite(control_data)] - mean) / std
        test_data[np.isfinite(test_data)] = \
            (test_data[np.isfinite(test_data)] - mean) / std
    return mean, std, control_data, test_data


def moving_average(a, n=5):
    ma = np.ma.masked_array(a, np.isnan(a))
    ret = np.cumsum(ma.filled(0))
    ret[n:] = ret[n:] - ret[:-n]
    counts = np.cumsum(~ma.mask)
    counts[n:] = counts[n:] - counts[:-n]
    ret[~ma.mask] /= counts[~ma.mask]
    ret[ma.mask] = np.nan

    return ret


# TODO implement even splitting
def smooth(w, data, metric_type):
    if metric_type == MetricType.COUNT:
        n = len(data)
        l = np.array_split(data, ceil(n / w)) if n >= w else data
        sums = [np.nansum(z) for z in l]
        return np.asarray(sums)
    else:
        n = len(data)
        l = np.array_split(data, ceil(n / w)) if n >= w else data
        means = [np.nanmean(z) for z in l]
        return np.asarray(means)


def simple_average(data, default_ret_val=np.nan):
    s = np.nansum(data)
    c = len(data[np.isfinite(data)])
    return default_ret_val if c == 0 else s / c
