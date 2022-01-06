# Copyright 2018 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import logging
import numpy as np
from collections import OrderedDict
from core.util.TimeSeriesUtils import MetricType


log_format = "%(asctime)-15s %(levelname)s %(message)s"
logging.basicConfig(level=logging.INFO, format=log_format)
logger = logging.getLogger(__name__)


def group_txns(analysis_minute, metric_names, transactions, metric_template, min_rpm, num_days):

    result = {}
    data_len = (analysis_minute + 1)
    for transaction in transactions:
        txn_name = transaction.get('name')
        host = transaction.get('host')
        data_collection_minute = transaction.get('dataCollectionMinute')
        day = 0

        if txn_name not in result:
            result[txn_name] = OrderedDict({})
            for metric_name in metric_names:
                result[txn_name][metric_name] = OrderedDict({})

        for metric_name in metric_names:
            if host not in result[txn_name][metric_name]:
                result[txn_name][metric_name][host] = OrderedDict({})
                result[txn_name][metric_name][host]['data'] = np.array([([np.nan] * data_len)] * num_days)
            if transaction.get(metric_name) is None or transaction.get(metric_name) == -1:
                continue

            result[txn_name][metric_name][host]['data'][day, data_collection_minute] = transaction.get(metric_name)


    return sanitize_data(result, metric_template, min_rpm)


def sanitize_data(txns_dict, metric_template, min_rpm_threshold):
    """

    Sanitize applies domain knowledge to embellish the collected data:

    It does 2 things:

    i) if the call count is 0, mark the average response time as unavailable, i.e set the data point
    to np.nan

    ii) if the requestsPerMinute is less than the min_rpm_threshold, mark the host as skipped. This flag will be used
    when deciding if the transaction is an anomaly. All hosts that are skipped will be not be considered when
    deciding if the transaction is anomalous later on.

    :param txns_dict: the transaction dictionary
    :param min_rpm_threshold: the minimum requests per minute threshold
    :return: embellished transaction dictionary
    """
    for txn_data_dict in txns_dict.values():
        for metric_name, metric_data_dict in txn_data_dict.items():
            if metric_template.get_metric_type(metric_name) == MetricType.RESP_TIME\
                     and metric_template.get_metric_name(MetricType.THROUGHPUT) is not None:
                throughput_metric_name = metric_template.get_metric_name(MetricType.THROUGHPUT)
                for host, metric_host_data_dict in metric_data_dict.items():
                    metric_host_data_dict['data'][np.where(txn_data_dict[throughput_metric_name][host]['data'] == 0)[0]] \
                        = np.nan
            if metric_template.get_metric_type(metric_name) == MetricType.THROUGHPUT:
                for host, metric_host_data_dict in metric_data_dict.items():
                    if np.nansum(metric_host_data_dict['data']) < min_rpm_threshold:
                        metric_host_data_dict['skip'] = True
                    else:
                        metric_host_data_dict['skip'] = False
    return txns_dict


def get_metrics_data(metric_name, metric_template, txn_data_dict):

    """
    extract data for the given metric_name from the transaction data dictionary
    for processing.

    {
        host_names: [list of hosts from which the metric was extracted],

        data: numpy 2d array of floats, which each subarray representing data collected from a node
                    [ [ host1 data ], [ host2 data ], [ host3 data ].....]

        data_type: histogram or count, useful when aggregating.
                   Ex: average a histogram metric, whereas sum a count based metric

        weights (optional): numpy 2d array of floats, that are the weights for the data points
                    [ [ host1 weights ], [ host2 weights ], [ host3 weights ].....],

        weights_type (optional): count
    }

    Weights are presently only used to scale the average response times by the call count. The intution is
    that the response times are more significant if the call counts are higher.

    :param metric_name: the name of the metric
    :param metric_template containing the metric configurations
    :param txn_data_dict: the transaction data dictionary
    :return: the data dictionary for processing.
    """
    if txn_data_dict is None or len(txn_data_dict) == 0:
        return {}

    weights = []
    data = []
    host_names = []

    for (host_name, host_data) in txn_data_dict[metric_name].items():
        host_names.append(host_name)
        data.append(host_data['data'].astype(np.float64))
    return dict(host_names=host_names, data=np.stack(data, axis=2),
                data_type=metric_template.get_metric_type(metric_name))


def validate(txn_name, metric_name, dict, type = 'control'):
    """
    Skip the metric for a transaction if there is no valid data.
    """
    control_valid = 'data' in dict and len(np.where(np.isfinite(dict['data'].flatten()))[0]) > 0
    # No useful control or test data.
    if not control_valid :
        logger.warn(
            "Skipping Txn -  " + txn_name + " , metric - " + metric_name + " No valid " + type + " data found!")
        return False
    else:
        return True



## questions :
## ask whether skip caused problems or not
