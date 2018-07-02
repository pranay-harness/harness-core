from __future__ import division
import argparse
import json
import sys
import time
from collections import OrderedDict
from Queue import Empty

import multiprocessing
import numpy as np

from core.distance.SAXHMMDistance import SAXHMMDistanceFinder
#from core.ts_neural_nets.auto_encoder import AutoEncoder
from core.util.TimeSeriesUtils import MetricType, simple_average, RiskLevel, MetricToDeviationType, \
    ThresholdComparisonType
from sources.HarnessLoader import HarnessLoader
from sources.MetricTemplate import MetricTemplate
from core.util.lelogging import get_log

logger = get_log(__name__)


class TSAnomlyDetector(object):

    def __init__(self, options, metric_template, control_txns, test_txns):
        self._options = options
        self.metric_template = MetricTemplate(metric_template)
        self.metric_names = self.metric_template.get_metric_names_by_tags()
        self.raw_control_txns = control_txns
        self.raw_test_txns = test_txns
        self.min_rpm = options.min_rpm

    def group_txns(self, transactions):
        """
        The input is a list of records collected by the delegate and sent to the harness server.
        Sample input record:
        {
            "stateExecutionId": "sWNcP394SP6YCn9AcbQ7bA",
            "lastUpdatedAt": 1508180932025,
            "applicationId": "pamgwigQRmiyuV5ePVWLsg",
            "createdAt": 1508180932025,
            "uuid": "7GerIpbVR7e18ZmtQlCLnA",
            "timeStamp": 1508180580000,
            "workflowId": "r3uYEEq-QP6pGh1SCIXiuA",
            "apdexScore": 1,
            "host": "ip-172-31-0-38",
            "serviceId": "NBKJds57Rv2c80NUShRZSQ",
            "averageResponseTime": 0,
            "createdBy": null,
            "appId": null,
            "callCount": 0,
            "requestsPerMinute": 0,
            "name": "WebTransaction/JSP/WEB-INF/jsp/_403.jsp",
            "level": null,
            "dataCollectionMinute": 0,
            "workflowExecutionId": "SLz58C52SWCzVqzi_YQDjg",
            "lastUpdatedBy": null,
            "throughput": 0,
            "error": 0,
            "tag":"Servlet"
          }
        Sample Output:
        Create a 3 level dictionary as shown:
            dict : transaction name (A)
                    : metric name (M1)
                            : host 1 => data
                            : host 2 => data
                    : metric name (B)
                            : host 1 => data
                            : host 2 => data
        In the above example the transaction name is "WebTransaction/JSP/WEB-INF/jsp/_403.jsp".
        For new relic the metric names are : ['callCount', 'averageResponseTime', 'requestsPerMinute', 'throughput', 'error', 'apdexScore']
        Non availability of data is denoted as np.Nan
        :param transactions: the list of transaction records
        :return: the grouped dictionary
        """
        result = {}
        data_len = (self._options.analysis_minute - self._options.analysis_start_min)+1
        txn_metrics = {}
        for transaction in transactions:
            # Strip the txnNames to make sure there are no leading or trailing spaces
            transaction['name'] = transaction['name'].strip()
            txn_name = transaction.get('name')
            if txn_name not in txn_metrics:
                txn_metrics[txn_name] = []
                tag = transaction.get('tag') if transaction.get('tag') else 'default'
                # Probably coming from a previous run
                if tag not in self.metric_names:
                    tag = 'default'
                for metric_name in self.metric_names[tag]:
                    if metric_name in transaction.get('values'):
                        txn_metrics[txn_name].append(metric_name)

        for transaction in transactions:
            txn_name = transaction.get('name')
            host = transaction.get('host')
            data_collection_minute = transaction.get('dataCollectionMinute') - self._options.analysis_start_min

            if txn_name not in result:
                result[txn_name] = OrderedDict({})
                for metric_name in txn_metrics[txn_name]:
                    if metric_name in transaction.get('values'):
                        result[txn_name][metric_name] = OrderedDict({})
            for metric_name in txn_metrics[txn_name]:
                if host not in result[txn_name][metric_name]:
                    result[txn_name][metric_name][host] = OrderedDict({})
                    result[txn_name][metric_name][host]['data'] = np.asarray([np.nan] * data_len)
                if transaction.get('values').get(metric_name) is None or transaction.get('values').get(metric_name) == -1:
                    continue
                result[txn_name][metric_name][host]['data'][data_collection_minute] = transaction.get('values').get(metric_name)

        return self.sanitize_data(result, self.metric_template, self.min_rpm)

    @staticmethod
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
                if metric_template.get_metric_type(metric_name) == MetricType.ERROR:
                    for host, metric_host_data_dict in metric_data_dict.items():
                        if not len(np.where(np.isfinite(metric_host_data_dict['data'].flatten()))[0]) > 0:
                            metric_host_data_dict['data'] = np.zeros(len(metric_host_data_dict['data']))


        return txns_dict

    @staticmethod
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
        if txn_data_dict is None or len(txn_data_dict) == 0 or metric_name not in txn_data_dict:
            return {}

        weights = []
        data = []
        host_names = []
        if metric_template.get_metric_type(metric_name) == MetricType.RESP_TIME \
                and metric_template.get_metric_name(MetricType.THROUGHPUT) is not None:
            throughput_metric_name = metric_template.get_metric_name(MetricType.THROUGHPUT)
            for (host_name, host_data) in txn_data_dict[metric_name].items():
                host_names.append(host_name)
                data.append(host_data['data'])
                weights.append(txn_data_dict[throughput_metric_name][host_name]['data'])
            return dict(host_names=host_names,
                        data=np.array(data, dtype=np.float64),
                        weights=np.array(weights, dtype=np.float64),
                        data_type=MetricType.RESP_TIME, weights_type=MetricType.COUNT)

        else:
            for (host_name, host_data) in txn_data_dict[metric_name].items():
                host_names.append(host_name)
                data.append(host_data['data'])
            return dict(host_names=host_names, data=np.array(data, dtype=np.float64),
                        data_type=metric_template.get_metric_type(metric_name))

    @staticmethod
    def validate(txn_name, metric_name, control_dict, test_dict, method = 'comparative'):
        """
        Skip the metric for a transaction if there is no valid data.
        """
        test_valid = len(np.where(np.isfinite(test_dict['data'].flatten()))[0]) > 0
        if method == 'predictive':
            control_valid = True
        else:
            control_valid = 'data' in control_dict and len(np.where(np.isfinite(control_dict['data'].flatten()))[0]) > 0

        # No useful control or test data.
        if not control_valid and not test_valid:
            logger.warn(
                "Skipping Txn -  " + txn_name + " ,metric - " + metric_name + " No valid control or test data found!")
            return False
        elif not control_valid:
            logger.warn(
                "Skipping txn - " + txn_name + " metric " + metric_name + " Missing control data found!")
            return False
        elif not test_valid:
            logger.warn("Skipping txn -  " + txn_name + " metric " + metric_name + " Missing test data!")
        else:
            return True

    def validate_prediction_input(self, txn_name, metric_name, data_dict):
        """
        Skip the metric for a transaction if there is no valid data.
        """
        valid = np.sum(~np.isnan(data_dict)) > 0
        if not valid :
            logger.warn(
                "Skipping prediction for Txn -  " + txn_name + " ,metric - " + metric_name + " No valid previous data!")
            return False
        else:
            return True

    @staticmethod
    def make_jsonable(txn_data_dict):
        """
        Replace np.nan with -1 and numpy arrays with a list, so we can
        create a json that is posted back to the Harness server.
        """
        txn_data_dict['data'][np.isnan(txn_data_dict['data'])] = -1
        txn_data_dict['data'] = txn_data_dict['data'].tolist()
        txn_data_dict['data_type'] = txn_data_dict['data_type'].value
        if 'weights' in txn_data_dict:
            txn_data_dict['weights'][np.isnan(txn_data_dict['weights'])] = -1
            txn_data_dict['weights'] = txn_data_dict['weights'].tolist()
            txn_data_dict['weights_type'] = txn_data_dict['weights_type'].value
        return txn_data_dict

    def fast_analysis_metric(self, num_point_per_bucket, transaction_name, metric_name, control_data_dict, test_data_dict):
            num_points = len(control_data_dict['data'][0])
            control_data_2d = np.array(control_data_dict['data'])
            test_data_2d = np.array(test_data_dict['data'])
            # when most of the data (70%) in a bucket are Nan, mean and std of the bucket is set to Nan
            base_array = np.asarray([np.nan] * num_points)
            std_array = np.asarray([np.nan] * num_points)
            if num_point_per_bucket >= num_points:
                bucket_data = control_data_2d
                if np.isnan(bucket_data).sum() < round(0.7*bucket_data.size):
                    base_array[:] = np.nanmean(bucket_data)
                    #if std of bucket is zero, it is set to 1
                    std_array[:] = 1 if np.nanstd(bucket_data) == 0 else np.nanstd(bucket_data)
            else:
                num_point_diff = num_points-num_point_per_bucket

                for idx in xrange(num_point_diff):
                    # if enough data (70%) is recorded it calculates mean otherwise mean remains Nan
                    bucket_data = control_data_2d[:, idx:idx+num_point_per_bucket]
                    if np.isnan(bucket_data).sum() < round(0.7*bucket_data.size):
                        base_array[idx] = np.nanmean(bucket_data)
                        #if std of bucket is zero, it is set to 1
                        std_array[idx] = 1 if np.nanstd(bucket_data) == 0 else np.nanstd(bucket_data)
                base_array[num_point_diff:] = \
                    base_array[num_point_diff-1]
                std_array[num_point_diff:] = \
                    std_array[num_point_diff-1]
            dist_2d_data = (test_data_2d - base_array)
            # if the difference is less than a specified minimum threshold, it is set to zero. This information is
            #  derived from domain knowledge. For instance, if the difference in average response time is less than
            # 50 ms, then it is acceptable regardless of what the distribution tells us.
            # Also, its low deviation if the % change is within a specified value. For now its at 50%
            min_delta = self.metric_template.get_deviation_threshold(metric_name, ThresholdComparisonType.DELTA)
            min_ratio = self.metric_template.get_deviation_threshold(metric_name, ThresholdComparisonType.RATIO)
            dist_2d_data[np.logical_or(abs(dist_2d_data) < min_delta,
                                       abs(dist_2d_data) < min_ratio * np.minimum(test_data_2d, base_array))] = 0
            # normalizing distances
            dist_2d_data = dist_2d_data/std_array
            # TODO this should be done after adjusting dist it should not be always positive
            # for points where baseline is Nan but test has a value, distance is set to maximum.
            dist_2d_data[np.logical_and(~np.isnan(test_data_2d), np.isnan(base_array))] = \
                3 * self._options.tolerance + 0.1
            metric_deviation_type = self.metric_template.get_deviation_type(metric_name)
            adjusted_dist = self.adjust_numeric_dist(metric_deviation_type, dist_2d_data)
            # clipping distances at threshold
            adjusted_dist[adjusted_dist > 3 * self._options.tolerance] = 3 * self._options.tolerance
            w_dist = []
            # weights has value just at points that test has a value
            cum_weight = np.sum(~np.isnan(test_data_2d), 1)
            cum_weight[cum_weight == 0] = 1
            for test_ind in xrange(len(adjusted_dist)):
                w_dist.append(np.sum(
                    [((p + 1) * v) for (p, v) in enumerate(adjusted_dist[test_ind]
                                                           [~ np.isnan(test_data_2d[test_ind, :])])]))
            w_dist /= ((cum_weight * (cum_weight+1))/2) * 1.
            risk = [0 if d < self._options.tolerance else 1 if d <= 2 * self._options.tolerance else 2 for d in w_dist]
            ## replacing all Nans coming from Nans of test data with 0
            adjusted_dist[np.isnan(test_data_2d)] = 0
            return dict(score=w_dist, risk=risk, control_values=base_array, distances=adjusted_dist,
                        test_values=test_data_dict['data'])

    def predictive_anlaysis(self, txn_name, metric_name, txn_data_dict, method='mean'):
        # TODO: winput window size should be customizable
        time_step_in = self._options.analysis_start_time
        response = {'results': {}, 'max_risk': -1}
        data_dict = self.get_metrics_data(metric_name, self.metric_template, txn_data_dict)
        if self.validate(txn_name, metric_name,
                         {}, data_dict, method='predictive'):
            host_name = data_dict['host_names'][0]

            num_points = len(data_dict['data'][0])
            deployment_index = self._options.analysis_start_time - self._options.analysis_start_min + 1
            #analysis_length = num_points - deployment_index
            start = deployment_index - time_step_in + 1
            end = deployment_index + 1
            pred_ind = end + 1
            # TODO: should remove 0 index input should be 1d
            all_data = np.copy(data_dict['data'])[0]
            input_data = np.copy(all_data)
            if self.validate_prediction_input(txn_name, metric_name, input_data[0:deployment_index]):


            # TODO if time_step_out is greater than it should predict more points and append to end
                weighted_dist = 0
                weight = 0
                anomalies = [0] * num_points
                while(pred_ind < num_points):
                    test_x = input_data[start:end]
                    if method == 'median':
                        y_hat = np.nanmedian(test_x)
                        mad = np.nanmedian(abs(test_x - y_hat))
                        std = 1.48 * mad
                    else:
                        y_hat = np.nanmean(test_x)
                        std = np.nanstd(test_x, ddof=1)
                        # with 1 degree of freedom std can be nan, it happens when there is
                        # just one none nan data point ,number of sample -1 would be zero
                        std = 0 if np.isnan(std) else std
                    # let 95% in
                    threshold = 1.96 * std
                    min_delta = self.metric_template.get_deviation_threshold(metric_name, ThresholdComparisonType.DELTA)
                    min_ratio = self.metric_template.get_deviation_threshold(metric_name, ThresholdComparisonType.RATIO)
                    y = input_data[pred_ind]
                    dist = y - y_hat
                    dist = 0 if abs(dist) < min_delta or abs(dist) < min_ratio * min(y, y_hat) else dist
                    # remove oulier for next time prediction
                    if abs(dist) >= threshold:
                        input_data[pred_ind] = y_hat  #+ np.sign(dist) * threshold
                    metric_deviation_type = self.metric_template.get_deviation_type(metric_name)
                    adjusted_dist = self.adjust_numeric_dist(metric_deviation_type, np.array([dist]))[0]
                    # need to normalize the dist to have std =1
                    adjusted_dist = adjusted_dist*1./std if std!=0 else adjusted_dist
                    # clipping distances at threshold assuming after normalization std =1
                    if adjusted_dist > 1.96:
                        # 1.96 is 95% and 2.57 is 99%
                        adjusted_dist = 2.67 if adjusted_dist > 2.57 else 2.06
                        anomalies[pred_ind] = 1
                    # keep distance if both predicted and measured have values
                    if not np.isnan(y) and not np.isnan(y_hat):
                        weight += 1
                        weighted_dist += adjusted_dist * weight
                    pred_ind += 1
                    end += 1
                    start = end - time_step_in
                if weight != 0:
                    weighted_dist /= ((weight * (weight+1))/2) * 1.
                    risk = 0 if weighted_dist <= 1.96 else 1 if weighted_dist <= 2.57 else 2
                else:
                    risk = -1

                throughput_metric_name = self.metric_template.get_metric_name(MetricType.THROUGHPUT)
                if throughput_metric_name is not None and throughput_metric_name in txn_data_dict \
                        and txn_data_dict[throughput_metric_name][host_name]['skip']:
                    final_risk = RiskLevel.NA.value
                else:
                    final_risk = risk


                response['results'][host_name] = dict(score=weighted_dist, risk=final_risk, test_data=np.ma.masked_array(all_data, np.isnan(all_data)).filled(-1).tolist(), anomalies=list(anomalies))
                response['max_risk'] = max(final_risk, response['max_risk'])
        if 'data' in data_dict:
            response['test_avg'] = simple_average(data_dict['data'].flatten(), -1)
            response['test'] = self.make_jsonable(data_dict)
        response['metric_name'] = metric_name

        return response

    def reshape_data(self, data, window_size):
        reshaped_data = []
        for i in range(len(data) - window_size):
            reshaped_data.append(data[i:i+window_size])
        return np.array(reshaped_data)

    def analyze_metric(self, txn_name, metric_name, control_txn_data_dict, test_txn_data_dict, max_nodes_threshold,
                       fast_analysis=None):

        control_data_dict = self.get_metrics_data(metric_name, self.metric_template, control_txn_data_dict)
        test_data_dict = self.get_metrics_data(metric_name, self.metric_template, test_txn_data_dict)
        response = {'results': {}, 'max_risk': -1, 'control_avg': -1, 'test_avg': -1}

        if self.validate(txn_name, metric_name,
                         control_data_dict, test_data_dict):
            if fast_analysis is None:
                fast_analysis = True if len(control_data_dict['data']) > max_nodes_threshold else False

            if fast_analysis:
                analysis_output = self.fast_analysis_metric(self._options.smooth_window, txn_name, metric_name, control_data_dict,
                                                            test_data_dict)
            else:
                data_len = (self._options.analysis_minute - self._options.analysis_start_min) + 1
                if data_len % self._options.smooth_window > 0:
                    pad_len = self._options.smooth_window - (data_len % self._options.smooth_window)
                    control_data_dict['data'] = np.pad(control_data_dict['data'], ((0, 0), (0, pad_len)), 'constant',
                                                       constant_values=np.nan)
                    test_data_dict['data'] = np.pad(test_data_dict['data'], ((0, 0), (0, pad_len)), 'constant',
                                                    constant_values=np.nan)
                shdf = SAXHMMDistanceFinder(transaction_name=txn_name,
                                            metric_name=metric_name,
                                            smooth_window=self._options.smooth_window,
                                            tolerance=self._options.tolerance,
                                            control_data_dict=control_data_dict,
                                            test_data_dict=test_data_dict,
                                            metric_template=self.metric_template,
                                            comparison_unit_window=self._options.comparison_unit_window)

                analysis_output = shdf.compute_dist()

            """
            Sample output , if fast method is  not used. For Fast Analysis some of parameters are not occupied:
            {
                "control" : {
                        "data" : [ [ host1 data], [ host2 data] ...],
                        "data_type" : 2, 2 is histogram
                        "weights" : [ [host1 data], [ host2 data]], 
                        "weights_type" : 1, 1 is count
                        "host_names" : [ 
                            "ip-172-31-0-38", 
                            "ip-172-31-11-65"
                        ]
                    },
                    "test" : {
                        "data" : [ [ host1 data], [ host2 data] ...],
                        "data_type" : 2,
                        "weights" : [ [host1 data], [ host2 data]],
                        "weights_type" : 1,
                        "host_names" : [ 
                            "ip-172-31-0-38", 
                            "ip-172-31-11-65"
                        ]
                    },
                    "results" : {
                        "ip-172-31-1-92" : {
                            "distance" : [
                                0.0
                            ],
                            "control_data" : [
                                267.333333333333
                            ],
                            "test_data" : [
                                302.0
                            ],
                            "control_cuts" : [
                                "c"
                            ],
                            "test_cuts" : [
                                "e"
                            ],
                            "optimal_cuts" : "",
                            "control_index" : 0,
                            "test_index" : 0,
                            "nn" : "ip-172-31-1-92",
                            "risk" : 0,
                            "score" : 0.0
                    }
                    "control_avg" : -1.0,
                    "test_avg" : -1.0,
                    "max_risk" : -1
            }    
            """
            for index, host in enumerate(test_txn_data_dict[metric_name].keys()):
                response['results'][host] = {}

                response['results'][host]['test_data'] = np.ma.masked_array(analysis_output['test_values'][index],
                                                                            np.isnan(
                                                                                analysis_output['test_values'][index])) \
                    .filled(-1).tolist()
                response['results'][host]['score'] = analysis_output['score'][index]
                response['results'][host]['distance'] = analysis_output['distances'][index].tolist()
                if fast_analysis:
                    response['results'][host]['control_data'] = np.ma.masked_array(
                        analysis_output['control_values'],
                        np.isnan(analysis_output['control_values'])).filled(-1).tolist()
                    response['results'][host]['nn'] = 'base'
                    response['results'][host]['control_index'] = 0
                else:
                    response['results'][host]['control_data'] = np.ma.masked_array(
                        analysis_output['control_values'][index],
                        np.isnan(analysis_output['control_values'][
                                     index])).filled(-1).tolist()

                    response['results'][host]['nn'] = control_txn_data_dict[metric_name].keys()[
                        analysis_output['nn'][index]]
                    response['results'][host]['test_cuts'] = analysis_output['test_cuts'][index].tolist()
                    response['results'][host]['optimal_cuts'] = analysis_output['optimal_test_cuts'][index]
                    response['results'][host]['optimal_data'] = np.ma.masked_array(
                        analysis_output['optimal_test_data'][index],
                        np.isnan(analysis_output['optimal_test_data'][
                                     index])).filled(
                        -1).tolist()
                    response['results'][host]['control_cuts'] = analysis_output['control_cuts'][index].tolist()
                    response['results'][host]['control_index'] = analysis_output['nn'][index]
                throughput_metric_name = self.metric_template.get_metric_name(MetricType.THROUGHPUT)
                if throughput_metric_name is not None and throughput_metric_name in test_txn_data_dict \
                        and test_txn_data_dict[throughput_metric_name][host]['skip']:
                    response['results'][host]['risk'] = RiskLevel.NA.value
                else:
                    response['results'][host]['risk'] = analysis_output['risk'][index]
                    response['max_risk'] = max(response['max_risk'], response['results'][host]['risk'])
                response['results'][host]['test_index'] = index
        if 'data' in control_data_dict:
            response['control_avg'] = simple_average(control_data_dict['data'].flatten(), -1)
            response['control'] = self.make_jsonable(control_data_dict)

        if 'data' in test_data_dict:
            response['test_avg'] = simple_average(test_data_dict['data'].flatten(), -1)
            response['test'] = self.make_jsonable(test_data_dict)

        response['metric_name'] = metric_name
        response['metric_type'] = self.metric_template.get_metric_type(metric_name).value
        response['alert_type'] = self.metric_template.get_deviation_type(metric_name).value

        return response

    @staticmethod
    def adjust_numeric_dist(metric_deviation_type, dist_2d_data):
        adjusted_dist = np.copy(dist_2d_data)
        if metric_deviation_type == MetricToDeviationType.HIGHER:
            adjusted_dist[dist_2d_data < 0] = 0
        elif metric_deviation_type == MetricToDeviationType.LOWER:
            adjusted_dist[dist_2d_data > 0] = 0
        return np.abs(adjusted_dist)

    @staticmethod
    def txn_meta_data_dict(transactions):
        txns_meta_dict = {}
        for transaction in transactions:
            if transaction.get('txn_name') not in txns_meta_dict:
                tag = transaction.get('tag') if transaction.get('tag') else 'default'
                txns_meta_dict[transaction.get('name')] = {'tag': tag}
        return txns_meta_dict

    def analyze(self, fast_analysis=None):
        """
         analyze all transaction / metric combinations
        """
        start_time = time.time()
        result = {'transactions': {}}

        control_txn_groups = self.group_txns(self.raw_control_txns)
        test_txn_groups = self.group_txns(self.raw_test_txns)

        test_txn_meta_data_dict = self.txn_meta_data_dict(self.raw_test_txns)

        txns_count = 0
        if self._options.time_series_ml_analysis_type == 'COMPARATIVE' and (len(control_txn_groups) == 0 or len(test_txn_groups) == 0):

            logger.warn(
                "No control or test data given for minute " + str(
                    self._options.analysis_minute) + ". Skipping analysis!!")
        elif self._options.time_series_ml_analysis_type == 'PREDICTIVE' and len(test_txn_groups) == 0:

            logger.warn(
                " data given for minute " + str(
                    self._options.analysis_minute) + ". Skipping predictive analysis!!")
        else:

            for txn_ind, (txn_name, test_txn_data_dict) in enumerate(test_txn_groups.items()):

                if txn_name in control_txn_groups:
                    control_txn_data_dict = control_txn_groups[txn_name]
                else:
                    control_txn_data_dict = {}

                for metric_ind, metric_name in enumerate(test_txn_data_dict.keys()):


                    if self._options.time_series_ml_analysis_type == 'COMPARATIVE':
                        logger.info("Comparative analyzing txn " + txn_name + " metric " + metric_name)
                        response = self.analyze_metric(txn_name, metric_name, control_txn_data_dict,
                                                       test_txn_data_dict, self._options.max_nodes_threshold,
                                                       fast_analysis)
                    else:
                        logger.info("Predictive analyzing txn " + txn_name + " metric " + metric_name)

                        response = self.predictive_anlaysis(txn_name, metric_name, test_txn_data_dict)
                    ''' Use numbers for dictionary keys to avoid a failure on the Harness manager side
                        when saving data to MongoDB. MongoDB does not allow 'dot' chars in the key names for
                        dictionaries, and transactions or metric names can contain the dot char.
                        So use numbers for keys and stick the name inside the dictionary. '''
                    if txn_ind not in result['transactions']:
                        result['transactions'][txn_ind] = dict(txn_name=txn_name, txn_tag=test_txn_meta_data_dict[txn_name].get('tag'), metrics={})

                    result['transactions'][txn_ind]['metrics'][metric_ind] = response
                txns_count += 1

        #print(json.dumps(result))
        logger.info('time taken ' + str(time.time() - start_time) + ' for # txns = ' + str(txns_count))
        return result

    # TODO: should be removed just for testing
    # def pred_analyze(self, fast_analysis=None):
    #     """
    #      analyze all transaction / metric combinations
    #     """
    #     start_time = time.time()
    #     result = {'transactions': {}}
    #
    #     control_txn_groups = self.group_txns(self.raw_control_txns)
    #     test_txn_groups = self.group_txns(self.raw_test_txns)
    #
    #     test_txn_meta_data_dict = self.txn_meta_data_dict(self.raw_test_txns)
    #
    #     txns_count = 0
    #     if len(control_txn_groups) == 0 or len(test_txn_groups) == 0:
    #         logger.warn(
    #             "No control or test data given for minute " + str(
    #                 self._options.analysis_minute) + ". Skipping analysis!!")
    #     else:
    #
    #         for txn_ind, (txn_name, test_txn_data_dict) in enumerate(control_txn_groups.items()):
    #             #if txn_name =='WebTransaction/RestWebService/executions/{workflowExecutionId} (GET)':
    #             if 10 < txns_count < 14:
    #
    #                 if txn_name in control_txn_groups:
    #                     control_txn_data_dict = control_txn_groups[txn_name]
    #                 else:
    #                     control_txn_data_dict = {}
    #
    #                 for metric_ind, metric_name in enumerate(test_txn_data_dict.keys()):
    #
    #                     logger.info("Analyzing txn " + txn_name + " metric " + metric_name)
    #                     response = self.nn_predictive_anlaysis(txn_name, metric_name, control_txn_data_dict)
    #
    #                     ''' Use numbers for dictionary keys to avoid a failure on the Harness manager side
    #                         when saving data to MongoDB. MongoDB does not allow 'dot' chars in the key names for
    #                         dictionaries, and transactions or metric names can contain the dot char.
    #                         So use numbers for keys and stick the name inside the dictionary. '''
    #                     if txn_ind not in result['transactions']:
    #                         result['transactions'][txn_ind] = dict(txn_name=txn_name, txn_tag=test_txn_meta_data_dict[txn_name].get('tag'), metrics={}, group_name=options.group_name)
    #
    #                     result['transactions'][txn_ind]['metrics'][metric_ind] = response
    #             txns_count += 1
    #
    #     #print(json.dumps(result))
    #     logger.info('time taken ' + str(time.time() - start_time) + ' for # txns = ' + str(txns_count))
    #     return result

''' End class TSA Anomlay detector'''



''' Helper methods begin here...'''


def load_from_harness_server(url, nodes, options):
    """
    load input from Harness server
    """
    raw_events = HarnessLoader.load_from_harness_raw_new(url, payload=dict(applicationId=options.appId,
                                                                    workflowId=options.workflow_id,
                                                                    workflowExecutionId=options.workflow_execution_id,
                                                                    stateExecutionId=options.state_execution_id,
                                                                    serviceId=options.service_id,
                                                                    analysisMinute=options.analysis_minute,
                                                                    analysisStartMinute=options.analysis_start_min,
                                                                    nodes=nodes),
                                                         version_file_path=options.version_file_path,
                                                         service_secret=options.service_secret)['resource']
    return raw_events


def load_metric_template_harness_server(url, options):
    """
    load metric template from Harness server
    """
    metric_template = HarnessLoader.load_from_harness_raw_new(url, payload={},
                                                              version_file_path=options.version_file_path,
                                                              service_secret=options.service_secret)['resource']
    return metric_template


def post_to_wings_server(options, results):
    """
    post response to Harness server
    """
    HarnessLoader.post_to_wings_server(options.analysis_save_url, json.dumps(results),
                                       version_file_path=options.version_file_path,
                                       service_secret=options.service_secret)


def parse(cli_args):
    """
     control_input_url: rest api url to get control data
     test_input_url: rest api url to get test data
     auth_token: authentication token to talk to Harness server
     application_id: the application id
     workflow_id: workflow id
     workflow_execution_id: workflow execution id
     service_id: service id
     control_nodes: nodes part of the control group
     test_nodes: nodes part of the test group
     state_execution_id: state execution id
     analysis_save_url: rest api to post the analysis to
     analysis_minute: minute being analyzed
     tolerance: number between 1 and 3
     smooth_window: smoothing window
     min_rpm: minimum requests per minute threshold
    """
    parser = argparse.ArgumentParser()
    parser.add_argument("--time_series_ml_analysis_type", type=str, required=True)
    parser.add_argument("--control_input_url", required=True)
    parser.add_argument("--test_input_url", required=True)
    parser.add_argument("--auth_token", required=True)
    parser.add_argument("--application_id", required=True)
    parser.add_argument("--workflow_id", required=True)
    parser.add_argument("--workflow_execution_id", required=True)
    parser.add_argument("--service_id", required=True)
    parser.add_argument("--control_nodes", nargs='+', type=str, required=True)
    parser.add_argument("--test_nodes", nargs='+', type=str, required=False)
    parser.add_argument("--state_execution_id", type=str, required=True)
    parser.add_argument("--analysis_save_url", required=True)
    parser.add_argument("--analysis_minute", type=int, required=True)
    parser.add_argument("--analysis_start_min", type=int, required=True)
    parser.add_argument("--analysis_start_time", type=int)
    parser.add_argument("--tolerance", type=int, required=True)
    parser.add_argument("--smooth_window", type=int, required=True)
    parser.add_argument("--min_rpm", type=int, required=True)
    parser.add_argument("--comparison_unit_window", type=int, required=True)
    parser.add_argument("--parallel_processes", type=int, required=True)
    parser.add_argument("--metric_template_url", type=str, required=True)
    parser.add_argument('--max_nodes_threshold', nargs='?', const=19, type=int, default=19)
    parser.add_argument("--version_file_path", required=True)
    parser.add_argument("--service_secret", required=True)
    return parser.parse_args(cli_args)


def analyze_parallel(queue, options, metric_template, control_metrics_batch, test_metrics_batch):
    """
    Method will be called in parallel. the result will be placed on 'queue'
    """
    try:
        anomaly_detector = TSAnomlyDetector(options, metric_template, control_metrics_batch, test_metrics_batch)
        queue.put(anomaly_detector.analyze())
    except Exception as e:
        logger.exception(e)
        raise Exception('Analysis failed for one of the workers')


def parallelize_processing(options, metric_template, control_metrics, test_metrics):
    """
    Break the work into parallel units. Each transaction and its metrics constitute a unit.
    The transactions will be run in upto 7 parallel processes. Once all processing is complete,
    the result is merged and returned.
    """
    result = {"transactions": {}}

    analysis_message = ""
    if options.time_series_ml_analysis_type != 'PREDICTIVE':
        if len(control_metrics) == 0 :
            analysis_message = "No control data found."
        if len(test_metrics) == 0 :
            analysis_message += " No test data found."

        if analysis_message != "" :
            logger.warn(
                "No control or test data given for minute " + str(
                    options.analysis_minute) + ". Skipping analysis!!")
            result['message'] = analysis_message + " Please check load. Skipping analysis!!"
            return result
    else:
        if len(test_metrics) == 0:
            logger.warn(
                "No data given for minute " + str(
                    options.analysis_minute) + ". Skipping predictive analysis!!")
            result['message'] = "No test data found. Skipping analysis!!"
            return result



    transaction_names = set()
    for transactions in control_metrics:
        transactions['name'] = transactions['name'].strip()
        transaction_names.add(transactions['name'])

    for transactions in test_metrics:
        transactions['name'] = transactions['name'].strip()
        transaction_names.add(transactions['name'])

    workers = min(options.parallel_processes, len(transaction_names))
    print('workers=', workers)
    transaction_names = list(transaction_names)
    control_metrics_batch = [[] for i in range(workers)]
    test_metrics_batch = [[] for i in range(workers)]
    jobs = []
    for transaction in control_metrics:
        control_metrics_batch[transaction_names.index(transaction['name']) % workers].append(transaction)

    for transaction in test_metrics:
        test_metrics_batch[transaction_names.index(transaction['name']) % workers].append(transaction)

    queue = multiprocessing.Queue()
    job_id = 0
    for i in range(workers):
        p = multiprocessing.Process(target=analyze_parallel,
                                    args=(queue, options, metric_template, control_metrics_batch[i], test_metrics_batch[i]))
        job_id = job_id + 1
        jobs.append(p)
        p.start()

    txn_id = 0
    processed = 0
    while processed < len(jobs):
        try:
            val = queue.get(timeout=120)
        except Empty:
            raise Exception("Timeout occured in worker process. Check logs")
        for txn_data in val['transactions'].values():
            result['transactions'][txn_id] = txn_data
            txn_id += 1
        processed = processed + 1

    return result

def write_to_file(filename, data):
    file_object = open(filename, "w")
    file_object.write(json.dumps(data))
    file_object.close()

def main(options):
    """
    load data from Harness Manager, run the anomaly detector,
    and post the results back to the Harness Manager.
    Sample metric template
    "averageResponseTime": {
        "metricName": "averageResponseTime",
        "thresholds": [
          {
            "thresholdType": "ALERT_WHEN_HIGHER",
            "comparisonType": "RATIO",
            "high": 1.5,
            "medium": 1.25,
            "min": 0.5
          },
          {
            "thresholdType": "ALERT_WHEN_HIGHER",
            "comparisonType": "DELTA",
            "high": 10,
            "medium": 5,
            "min": 50
          }
        ],
        "metricType": "RESP_TIME"
    }
    """
    try:
        logger.info("Running Time Series analysis ")

        metric_template = load_metric_template_harness_server(options.metric_template_url, options)
        logger.info('metric_template = ' + json.dumps(metric_template))

        if options.time_series_ml_analysis_type == 'COMPARATIVE':
            control_metrics = load_from_harness_server(options.control_input_url, options.control_nodes, options)
            logger.info('control events = ' + str(len(control_metrics)))
        else:
            control_metrics = {}

        test_metrics = load_from_harness_server(options.test_input_url, options.test_nodes, options)
        logger.info('test_events = ' + str(len(test_metrics)))

        # Uncomment when you want to save the files for local debugging

        # write_to_file('/Users/parnianzargham/Desktop/portal/python/splunk_intelligence/time_series/test_live_queue.json', test_metrics)
        # write_to_file('/Users/parnianzargham/Desktop/portal/python/splunk_intelligence/time_series/control_live_queue.json',control_metrics)

        #write_to_file('/Users/parnianzargham/Desktop/wings/python/splunk_intelligence/time_series/test_live_new4.json', test_metrics)
        #
        #write_to_file('/Users/parnianzargham/Desktop/wings/python/splunk_intelligence/time_series/control_live_new4.json',control_metrics)
        #

        result = parallelize_processing(options, metric_template, control_metrics, test_metrics)
        #write_to_file('/Users/parnianzargham/Desktop/portal/python/splunk_intelligence/time_series/result_new_test_queue4.json', result)

        post_to_wings_server(options, result)
    except Exception as e:
        payload = dict(applicationId=options.appId,
                       workflowId=options.workflow_id,
                       workflowExecutionId=options.workflow_execution_id,
                       stateExecutionId=options.state_execution_id,
                       serviceId=options.service_id,
                       analysisMinute=options.analysis_minute)
        logger.exception(e)
        raise Exception('Analysis failed for ' + json.dumps(payload))


if __name__ == "__main__":
    args = sys.argv
    logger.info(args)
    options = parse(args[1:])
    logger.info(options)
    main(options)


    ## what to add to options:
    #self._options.time_step_in prediction input window
    #self._options.deployment_time
    # self._options.time_step_out for mean is 1