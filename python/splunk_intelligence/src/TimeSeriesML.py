from __future__ import division

import argparse
import json
import logging
import sys
import time
from collections import OrderedDict
from datetime import datetime, timedelta

import numpy as np

from core.distance.SAXHMMDistance import SAXHMMDistanceFinder
from core.util.TimeSeriesUtils import MetricType, get_deviation_type, simple_average, get_deviation_min_threshold
from sources.NewRelicSource import NewRelicSource
from sources.HarnessLoader import HarnessLoader

format = "%(asctime)-15s %(levelname)s %(message)s"
logging.basicConfig(level=logging.INFO, format=format)
logger = logging.getLogger(__name__)


class TSAnomlyDetector(object):
    # name, id, data_collection_minute, host, timestamp,
    # throughput, averageResponseTime, error, apdexScore

    def __init__(self, options, control_txns, test_txns):
        self._options = options
        self.raw_control_txns = control_txns
        self.raw_test_txns = test_txns
        self.metric_names = ['callCount', 'averageResponseTime', 'requestsPerMinute', 'throughput', 'error', 'apdexScore']
        self.min_rpm = options.min_rpm

    def group_txns(self, transactions):
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
    def sanitize_data(txns, min_rpm):
        for txn_data_dict in txns.values():
            for metric_name, metric_data_dict in txn_data_dict.items():
                if metric_name == 'averageResponseTime':
                    for host, metric_host_data_dict in metric_data_dict.items():
                        metric_host_data_dict['data'][np.where(txn_data_dict['callCount'][host]['data'] == 0)[0]] \
                            = np.nan
                if metric_name == 'requestsPerMinute':
                    for host, metric_host_data_dict in metric_data_dict.items():
                        if np.nansum(metric_host_data_dict['data']) < min_rpm:
                            metric_host_data_dict['skip'] = True
                        else:
                            metric_host_data_dict['skip'] = False
        return txns

    @staticmethod
    def get_metrics_data(metric_name, txn_data_dict):

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
            return dict(host_names=host_names, data=np.array(data, dtype=np.float64),
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

            tsd = SAXHMMDistanceFinder(metric_name=metric_name,
                                       smooth_window=self._options.smooth_window,
                                       tolerance=self._options.tolerance,
                                       control_data_dict=control_data_dict,
                                       test_data_dict=test_data_dict,
                                       metric_deviation_type=get_deviation_type(metric_name),
                                       min_metric_threshold=get_deviation_min_threshold(metric_name))

            result = tsd.compute_dist()

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
        start_time = time.time()
        result = {'transactions': {}}
        control_txn_groups = self.group_txns(self.raw_control_txns)
        test_txn_groups = self.group_txns(self.raw_test_txns)

        if len(control_txn_groups) == 0 or len(test_txn_groups) == 0:
            logger.warn(
                "No control or test data given for minute " + str(self._options.analysis_minute) + ". Skipping analysis!!")
        else:

            for txn_ind, (txn_name, test_txn_data_dict) in enumerate(test_txn_groups.items()):

                if txn_name in control_txn_groups:
                    control_txn_data_dict = control_txn_groups[txn_name]
                else:
                    control_txn_data_dict = {}

                for metric_ind, metric_name in enumerate(self.metric_names):

                    if metric_name == 'callCount':
                        continue

                    logger.info("Analyzing txn " + txn_name + " metric " + metric_name)

                    response = self.analyze_metric(txn_name, metric_name, control_txn_data_dict, test_txn_data_dict)

                    if txn_ind not in result['transactions']:
                        result['transactions'][txn_ind] = dict(txn_name=txn_name, metrics={})

                    result['transactions'][txn_ind]['metrics'][metric_ind] = response

        #print(json.dumps(result))
        #print(time.time() - start_time)
        return result


def load_from_harness_server(url, nodes, options):
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
    HarnessLoader.post_to_wings_server(options.analysis_save_url, options.auth_token,
                                             json.dumps(results))


def parse(cli_args):
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
    parser.add_argument("--debug", required=False)
    parser.add_argument("--min_rpm", type=int, required=True)

    return parser.parse_args(cli_args)


def run_debug():
    parser = argparse.ArgumentParser()
    parser.add_argument("--analysis_minute", type=int, required=True)
    parser.add_argument("--tolerance", type=int, required=True)
    parser.add_argument("--smooth_window", type=int, required=True)
    parser.add_argument("--min_rpm", type=int, required=True)
    options = parser.parse_args(['--analysis_minute', '30', '--tolerance', '1', '--smooth_window', '3',
                                 '--min_rpm', '10'])

    logger.info("Running Time Series analysis ")
    with open("/Users/sriram_parthasarathy/wings/python/splunk_intelligence/time_series/control_live.json",
              'r') as read_file:
        control_metrics = json.loads(read_file.read())
    with open("/Users/sriram_parthasarathy/wings/python/splunk_intelligence/time_series/test_live.json",
              'r') as read_file:
        test_metrics = json.loads(read_file.read())
    anomaly_detector = TSAnomlyDetector(options, control_metrics, test_metrics)
    anomaly_detector.analyze()


def write_to_file(filename, data):
    file_object = open(filename, "w")
    file_object.write(json.dumps(data))
    file_object.close()


def run_live():
    source = NewRelicSource(56513566)
    to_time = datetime.utcnow()
    from_time = to_time - timedelta(minutes=30)
    parser = argparse.ArgumentParser()
    parser.add_argument("--analysis_minute", type=int, required=True)
    parser.add_argument("--tolerance", type=int, required=True)
    parser.add_argument("--smooth_window", type=int, required=True)
    parser.add_argument("--min_rpm", type=int, required=True)
    options = parser.parse_args(['--analysis_minute', '30', '--tolerance', '1', '--smooth_window', '3',
                                 '--min_rpm', '10'])
    control_data, test_data = source.live_analysis({'ip-172-31-8-144', 'ip-172-31-12-79', 'ip-172-31-1-92'},
                                                   {'ip-172-31-13-153'}, from_time,
                                                   to_time)
    write_to_file('/Users/sriram_parthasarathy/wings/python/splunk_intelligence/time_series/test_live.json', test_data)
    write_to_file('/Users/sriram_parthasarathy/wings/python/splunk_intelligence/time_series/control_live.json',
                  control_data)
    anomaly_detector = TSAnomlyDetector(options, control_data, test_data)
    result = anomaly_detector.analyze()


def main(args):
    if len(args) > 1 and args[1] == 'debug':
        run_debug()
        exit(0)
    elif len(args) > 1 and args[1] == 'live':
        run_live()
        exit(0)
    else:
        logger.info(args)

        options = parse(args[1:])
        logger.info(options)

        if options.debug:
            run_debug()
            return

        logger.info("Running Time Series analysis ")

        control_metrics = load_from_harness_server(options.control_input_url, options.control_nodes, options)
        logger.info('control events = ' + str(len(control_metrics)))
        # write_to_file("/Users/sriram_parthasarathy/wings/python/splunk_intelligence/time_series/control_live.json",
        #               control_metrics)
        test_metrics = load_from_harness_server(options.test_input_url, options.test_nodes, options)
        logger.info('test_events = ' + str(len(test_metrics)))
        # write_to_file("/Users/sriram_parthasarathy/wings/python/splunk_intelligence/time_series/test_live.json",
        #               test_metrics)
        anomaly_detector = TSAnomlyDetector(options, control_metrics, test_metrics)
        result = anomaly_detector.analyze()
        post_to_wings_server(options, result)


if __name__ == "__main__":
    main(sys.argv)
