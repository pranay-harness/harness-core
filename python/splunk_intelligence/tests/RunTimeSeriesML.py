import argparse
import json

from datetime import timedelta, datetime

import sys
import logging
from TimeSeriesML import TSAnomlyDetector
from sources.NewRelicSource import NewRelicSource

log_format = "%(asctime)-15s %(levelname)s %(message)s"
logging.basicConfig(level=logging.INFO, format=log_format)
logger = logging.getLogger(__name__)


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
    control_data, test_data = source.live_analysis({'ip-172-31-8-144', 'ip-172-31-12-79'},
                                                   {'ip-172-31-13-153'}, from_time,
                                                   to_time)
    write_to_file('/Users/sriram_parthasarathy/wings/python/splunk_intelligence/time_series/test_live.json', test_data)
    write_to_file('/Users/sriram_parthasarathy/wings/python/splunk_intelligence/time_series/control_live.json',
                  control_data)
    anomaly_detector = TSAnomlyDetector(options, control_data, test_data)
    result = anomaly_detector.analyze()
    print(json.dumps(result))


def main(args):
    if len(args) > 1 and args[1] == 'debug':
        run_debug()
        exit(0)
    elif len(args) > 1 and args[1] == 'live':
        run_live()
        exit(0)

if __name__ == "__main__":
    main(sys.argv)