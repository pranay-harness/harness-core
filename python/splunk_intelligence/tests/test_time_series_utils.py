# Copyright 2017 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

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
