# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import argparse
import json
import sys
from TimeSeriesML import TSAnomlyDetector, parallelize_processing
from core.util.lelogging import get_log
from datetime import timedelta, datetime
from sources.FileLoader import FileLoader
from sources.NewRelicSource import NewRelicSource

logger = get_log(__name__)

parser = argparse.ArgumentParser()
parser.add_argument("--analysis_minute", type=int, required=True)
parser.add_argument("--tolerance", type=int, required=True)
parser.add_argument("--smooth_window", type=int, required=True)
parser.add_argument("--min_rpm", type=int, required=True)
parser.add_argument("--comparison_unit_window", type=int, required=True)
parser.add_argument("--parallelProcesses", type=int, required=True)
parser.add_argument('--max_nodes_threshold', nargs='?', const=19, type=int)

metric_template = FileLoader.load_data('resources/ts/metric_template.json')

def run_load():
    control_nodes = ['control-' + str(i) for i in range(50)]
    test_nodes = ['test-' + str(i) for i in range(12)]
    options = parser.parse_args(['--analysis_minute', '30', '--tolerance', '1', '--smooth_window', '3',
                                 '--min_rpm', '10', '--comparison_unit_window', '3', '--parallelProcesses', '2',
                                 '--max_nodes_threshold', '19'])

    logger.info("Running Time Series analysis ")
    with open("/Users/sriram_parthasarathy/wings/python/splunk_intelligence/time_series/control_live.json",
              'r') as read_file:
        control_metrics = json.loads(read_file.read())

    fake_control_metrics = []
    for metric in control_metrics:
        if metric['host'] == 'ip-172-31-8-144':
            for control_node in control_nodes:
                fake_control_metrics.append({"requestsPerMinute": metric["requestsPerMinute"],
                                     "name": metric["name"],
                                     "dataCollectionMinute": metric["dataCollectionMinute"],
                                     "apdexScore": metric["apdexScore"],
                                     "host": control_node,
                                     "averageResponseTime": metric["averageResponseTime"],
                                     "error": metric["error"],
                                     "throughput": metric["throughput"],
                                     "callCount": 492})
    control_metrics.extend(fake_control_metrics)


    with open("/Users/sriram_parthasarathy/wings/python/splunk_intelligence/time_series/test_live.json",
              'r') as read_file:
        test_metrics = json.loads(read_file.read())

    fake_test_metrics = []
    for metric in test_metrics:
        if metric['host'] == 'ip-172-31-13-153':
            for test_node in test_nodes:
                fake_test_metrics.append({"requestsPerMinute": metric["requestsPerMinute"],
                                             "name": metric["name"],
                                             "dataCollectionMinute": metric["dataCollectionMinute"],
                                             "apdexScore": metric["apdexScore"],
                                             "host": test_node,
                                             "averageResponseTime": metric["averageResponseTime"],
                                             "error": metric["error"],
                                             "throughput": metric["throughput"],
                                             "callCount": 492})
    test_metrics.extend(fake_test_metrics)

    parallelize_processing(options, metric_template, control_metrics, test_metrics)
    # anomaly_detector = TSAnomlyDetector(options, control_metrics, test_metrics)
    # anomaly_detector.analyze()


def run_debug():
    options = parser.parse_args(['--analysis_minute', '30', '--tolerance', '1', '--smooth_window', '3',
                                 '--min_rpm', '10', '--comparison_unit_window', '3', '--parallelProcesses', '2',
                                 '--max_nodes_threshold', '19'])

    logger.info("Running Time Series analysis ")
    with open("/Users/sriram_parthasarathy/wings/python/splunk_intelligence/time_series/control_live.json",
              'r') as read_file:
        control_metrics = json.loads(read_file.read())
    with open("/Users/sriram_parthasarathy/wings/python/splunk_intelligence/time_series/test_live.json",
              'r') as read_file:
        test_metrics = json.loads(read_file.read())
    anomaly_detector = TSAnomlyDetector(options, metric_template, control_metrics, test_metrics)
    print(json.dumps(anomaly_detector.analyze()))


def write_to_file(filename, data):
    file_object = open(filename, "w")
    file_object.write(json.dumps(data))
    file_object.close()


def run_live():
    source = NewRelicSource(56513566)
    to_time = datetime.utcnow()
    from_time = to_time - timedelta(minutes=30)

    options = parser.parse_args(['--analysis_minute', '45', '--tolerance', '1', '--smooth_window', '3',
                                 '--min_rpm', '10', '--comparison_unit_window', '3', '--parallelProcesses', '2',
                                 '--max_nodes_threshold', '19'])
    control_data, test_data = source.live_analysis({'ip-172-31-8-144', 'ip-172-31-12-79'},
                                                   {'ip-172-31-13-153'}, from_time,
                                                   to_time)
    write_to_file('/Users/sriram_parthasarathy/wings/python/splunk_intelligence/time_series/test_live_1.json', test_data)
    write_to_file('/Users/sriram_parthasarathy/wings/python/splunk_intelligence/time_series/control_live_1.json',
                  control_data)
    anomaly_detector = TSAnomlyDetector(options, metric_template, control_data, test_data)
    result = anomaly_detector.analyze()
    print(json.dumps(result))


def main(args):
    if len(args) > 1 and args[1] == 'debug':
        run_debug()
        exit(0)
    elif len(args) > 1 and args[1] == 'live':
        run_live()
        exit(0)
    elif len(args) > 1 and args[1] == 'load':
        run_load()
        exit(0)

if __name__ == "__main__":
    main(sys.argv)
