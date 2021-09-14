# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0

import sys

from core.util.TimeSeriesUtils import smooth, MetricType


def test_smooth():
    print(smooth(3, [ 0.878, 791.0, 376.0, 1.18, 0.959, 0.5], MetricType.RESP_TIME))


def test_smooth_1():
    print(smooth(3, [-1.0, -1.0, -1.0, -1.0, -1.0, -1.0, 29.7, 29.7, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0,
         -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0, -1.0], MetricType.RESP_TIME))


def main(args):
    test_smooth_1()


if __name__ == "__main__":
    main(sys.argv)
