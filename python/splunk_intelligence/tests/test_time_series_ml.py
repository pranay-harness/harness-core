import argparse
import numpy as np

import sys

from TimeSeriesML import TSAnomlyDetector
from sources.SplunkFileSource import SplunkFileSource

parser = argparse.ArgumentParser()
parser.add_argument("--analysis_minute", type=int, required=True)
parser.add_argument("--tolerance", type=int, required=True)
parser.add_argument("--smooth_window", type=int, required=True)
options = parser.parse_args(['--analysis_minute', '0', '--tolerance', '1', '--smooth_window', '3'])


def test_load_input():
    control = SplunkFileSource.load_data('resources/ts/NRSampleInput.json')
    anomaly_detector = TSAnomlyDetector(options, control, control)
    anomaly_detector.analyze()

def test_run_1():
    control = SplunkFileSource.load_data('resources/ts/NRSampleControl1.json')
    test = SplunkFileSource.load_data('resources/ts/NRSampleTest1.json')
    anomaly_detector = TSAnomlyDetector(options, control, test)
    anomaly_detector.analyze()


def main(args):
    print(np.nanmean([np.nan, np.nan]))
    test_run_1()


if __name__ == "__main__":
    main(sys.argv)