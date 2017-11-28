from __future__ import division

import argparse
import hashlib
import json
import logging
import sys
import time
from collections import OrderedDict

import multiprocessing
import numpy as np

from core.distance.SAXHMMDistance import SAXHMMDistanceFinder
from core.util.TimeSeriesUtils import MetricType, get_deviation_type, simple_average, get_deviation_min_threshold
from sources.HarnessLoader import HarnessLoader

log_format = "%(asctime)-15s %(levelname)s %(message)s"
logging.basicConfig(level=logging.INFO, format=log_format)
logger = logging.getLogger(__name__)


class TSAnomlyDetector(object):

    def __init__(self, options, control_txns, test_txns):
        self._options = options
        self.raw_control_txns = control_txns
        self.raw_test_txns = test_txns
        self.metric_names = ['callCount', 'averageResponseTime', 'requestsPerMinute', 'throughput', 'error', 'apdexScore']
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
            "error": 0
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
        for transaction in transactions:
            txn_name = transaction.get('name')
            host = transaction.get('host')
            data_collection_minute = transaction.get('dataCollectionMinute')

            if txn_name not in result:
                result[txn_name] = OrderedDict({})
                for metric_name in self.metric_names:
                    result[txn_name][metric_name] = OrderedDict({})

            for metric_name in self.metric_names:
                if host not in result[txn_name][metric_name]:
                    result[txn_name][metric_name][host] = OrderedDict({})
                    result[txn_name][metric_name][host]['data'] = np.asarray([np.nan] * (self._options.analysis_minute + 1))
                if transaction.get(metric_name) is None or transaction.get(metric_name) == -1:
                    continue
                result[txn_name][metric_name][host]['data'][data_collection_minute] = transaction.get(metric_name)

        return self.sanitize_data(result, self.min_rpm)

    @staticmethod
    def sanitize_data(txns_dict, min_rpm_threshold):
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
                if metric_name == 'averageResponseTime':
                    for host, metric_host_data_dict in metric_data_dict.items():
                        metric_host_data_dict['data'][np.where(txn_data_dict['callCount'][host]['data'] == 0)[0]] \
                            = np.nan
                if metric_name == 'requestsPerMinute':
                    for host, metric_host_data_dict in metric_data_dict.items():
                        if np.nansum(metric_host_data_dict['data']) < min_rpm_threshold:
                            metric_host_data_dict['skip'] = True
                        else:
                            metric_host_data_dict['skip'] = False
        return txns_dict

    @staticmethod
    def get_metrics_data(metric_name, txn_data_dict):

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
        :param txn_data_dict: the transaction data dictionary
        :return: the data dictionary for processing.
        """
        if txn_data_dict is None or len(txn_data_dict) == 0:
            return {}

        weights = []
        data = []
        host_names = []
        if metric_name == 'averageResponseTime':
            for (host_name, host_data) in txn_data_dict[metric_name].items():
                host_names.append(host_name)
                data.append(host_data['data'])
                weights.append(txn_data_dict['callCount'][host_name]['data'])
            return dict(host_names=host_names,
                        data=np.array(data, dtype=np.float64),
                        weights=np.array(weights, dtype=np.float64),
                        data_type=MetricType.HISTOGRAM, weights_type=MetricType.COUNT)

        else:
            for (host_name, host_data) in txn_data_dict[metric_name].items():
                host_names.append(host_name)
                data.append(host_data['data'])
            return dict(host_names=host_names, data=np.array(data, dtype=np.float64),
                        data_type=MetricType.HISTOGRAM)

    @staticmethod
    def validate(txn_name, metric_name, control_dict, test_dict):
        """
        Skip the metric for a transaction if there is no valid data.
        """
        control_valid = 'data' in control_dict and len(np.where(np.isfinite(control_dict['data'].flatten()))[0]) > 0
        test_valid = 'data' in test_dict and len(np.where(np.isfinite(test_dict['data'].flatten()))[0]) > 0
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

    def analyze_metric(self, txn_name, metric_name, control_txn_data_dict, test_txn_data_dict):

        control_data_dict = self.get_metrics_data(metric_name, control_txn_data_dict)
        test_data_dict = self.get_metrics_data(metric_name, test_txn_data_dict)

        response = {'results': {}, 'max_risk': -1, 'control_avg': -1, 'test_avg': -1}

        if self.validate(txn_name, metric_name,
                         control_data_dict, test_data_dict):

            shdf = SAXHMMDistanceFinder(metric_name=metric_name,
                                        smooth_window=self._options.smooth_window,
                                        tolerance=self._options.tolerance,
                                        control_data_dict=control_data_dict,
                                        test_data_dict=test_data_dict,
                                        metric_deviation_type=get_deviation_type(metric_name),
                                        min_metric_threshold=get_deviation_min_threshold(metric_name),
                                        comparison_unit_window=self._options.comparison_unit_window)

            result = shdf.compute_dist()

            """
            Sample output:
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
                response['results'][host]['control_data'] = np.ma.masked_array(result['control_values'][index],
                                                                            np.isnan(result['control_values'][
                                                                                         index])).filled(0).tolist()
                response['results'][host]['test_data'] = np.ma.masked_array(result['test_values'][index],
                                                                     np.isnan(result['test_values'][index])).filled(
                    0).tolist()
                response['results'][host]['distance'] = result['distances'][index].tolist()
                response['results'][host]['nn'] = control_txn_data_dict[metric_name].keys()[
                    result['nn'][index]]
                response['results'][host]['score'] = result['score'][index]
                response['results'][host]['test_cuts'] = result['test_cuts'][index].tolist()
                response['results'][host]['optimal_cuts'] = result['optimal_test_cuts'][index]
                response['results'][host]['optimal_data'] = np.ma.masked_array(result['optimal_test_data'][index],
                                                                        np.isnan(result['optimal_test_data'][
                                                                                index])).filled(
                    0).tolist()
                response['results'][host]['control_cuts'] = result['control_cuts'][index].tolist()
                if test_txn_data_dict['requestsPerMinute'][host]['skip']:
                    # TODO -1 implies risk NA. Make this an enum
                    response['results'][host]['risk'] = -1
                else:
                    response['results'][host]['risk'] = result['risk'][index]
                    response['max_risk'] = max(response['max_risk'], response['results'][host]['risk'])
                response['results'][host]['control_index'] = result['nn'][index]
                response['results'][host]['test_index'] = index

        if 'data' in control_data_dict:
            response['control_avg'] = simple_average(control_data_dict['data'].flatten(), -1)
            response['control'] = self.make_jsonable(control_data_dict)

        if 'data' in test_data_dict:
            response['test_avg'] = simple_average(test_data_dict['data'].flatten(), -1)
            response['test'] = self.make_jsonable(test_data_dict)

        response['metric_name'] = metric_name

        return response

    def analyze(self):
        """
         analyze all transaction / metric combinations
        """
        start_time = time.time()
        result = {'transactions': {}}
        control_txn_groups = self.group_txns(self.raw_control_txns)
        test_txn_groups = self.group_txns(self.raw_test_txns)

        txns_count = 0
        if len(control_txn_groups) == 0 or len(test_txn_groups) == 0:
            logger.warn(
                "No control or test data given for minute " + str(
                    self._options.analysis_minute) + ". Skipping analysis!!")
        else:

            for txn_ind, (txn_name, test_txn_data_dict) in enumerate(test_txn_groups.items()):

                if txn_name in control_txn_groups:
                    control_txn_data_dict = control_txn_groups[txn_name]
                else:
                    control_txn_data_dict = {}

                for metric_ind, metric_name in enumerate(self.metric_names):

                    # Not useful to analyze callCount by itself
                    if metric_name == 'callCount':
                        continue

                    logger.info("Analyzing txn " + txn_name + " metric " + metric_name)

                    response = self.analyze_metric(txn_name, metric_name, control_txn_data_dict, test_txn_data_dict)

                    # The numbers for dictionary keys is to avoid a failure on the Harness manager side
                    # when saving data to Mongo. Mongo does not allow dot chars in the key names for
                    # dictionaries, and transactions or metric names can contain the dot char.
                    # so use numbers for keys and stick the name inside the dictionary.
                    if txn_ind not in result['transactions']:
                        result['transactions'][txn_ind] = dict(txn_name=txn_name, metrics={})

                    result['transactions'][txn_ind]['metrics'][metric_ind] = response
                txns_count += 1

        #print(json.dumps(result))
        logger.info('time taken ' + str(time.time() - start_time) + ' for # txns = ' + str(txns_count))
        return result

''' End class TSA Anomlay detector'''



''' Helper methods begin here...'''


def load_from_harness_server(url, nodes, options):
    """
    load input from Harness server
    """
    raw_events = HarnessLoader.load_from_harness_raw_new(url,
                                                               options.auth_token,
                                                               dict(applicationId=options.application_id,
                                                                    workflowId=options.workflow_id,
                                                                    workflowExecutionId=options.workflow_execution_id,
                                                                    stateExecutionId=options.state_execution_id,
                                                                    serviceId=options.service_id,
                                                                    analysisMinute=options.analysis_minute,
                                                                    nodes=nodes))['resource']
    return raw_events


def post_to_wings_server(options, results):
    """
    post response to Harness server
    """
    HarnessLoader.post_to_wings_server(options.analysis_save_url, options.auth_token,
                                             json.dumps(results))


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
    parser.add_argument("--control_input_url", required=True)
    parser.add_argument("--test_input_url", required=False)
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
    parser.add_argument("--tolerance", type=int, required=True)
    parser.add_argument("--smooth_window", type=int, required=True)
    parser.add_argument("--min_rpm", type=int, required=True)
    parser.add_argument("--comparison_unit_window", type=int, required=True)
    parser.add_argument("--parallel_processes", type=int, required=True)

    return parser.parse_args(cli_args)


def analyze_parallel(queue, options, control_metrics_batch, test_metrics_batch):
    """
    Method will be called in parallel. the result will be placed on 'queue'
    """
    anomaly_detector = TSAnomlyDetector(options, control_metrics_batch, test_metrics_batch)
    queue.put(anomaly_detector.analyze())


def parallelize_processing(options, control_metrics, test_metrics):
    """
    Break the work into parallel units. Each transaction and its metrics constitute a unit.
    The transactions will be run in upto 7 parallel processes. Once all processing is complete,
    the result is merged and returned.
    """
    transaction_names = set()
    for transactions in control_metrics:
        transaction_names.add(transactions['name'])

    for transactions in test_metrics:
        transaction_names.add(transactions['name'])

    workers = min(options.parallel_processes, len(transaction_names))

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
                                    args=(queue, options, control_metrics_batch[i], test_metrics_batch[i]))
        job_id = job_id + 1
        jobs.append(p)
        p.start()

    result = {"transactions": {}}
    txn_id = 0
    processed = 0
    while processed < len(jobs):
        # TODO - get blocks forever. Will java kill us?
        val = queue.get()
        for txn_data in val['transactions'].values():
            result['transactions'][txn_id] = txn_data
            txn_id += 1
        processed = processed + 1

    return result


def main(args):
    """
    load data from Harness Manager, run the anomaly detector,
    and post the results back to the Harness Manager.

    """
    logger.info(args)

    options = parse(args[1:])
    logger.info(options)

    logger.info("Running Time Series analysis ")

    control_metrics = load_from_harness_server(options.control_input_url, options.control_nodes, options)
    logger.info('control events = ' + str(len(control_metrics)))

    test_metrics = load_from_harness_server(options.test_input_url, options.test_nodes, options)
    logger.info('test_events = ' + str(len(test_metrics)))

    result = parallelize_processing(options, control_metrics, test_metrics)
    post_to_wings_server(options, result)


if __name__ == "__main__":
    main(sys.argv)
