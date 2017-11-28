import sys

from core.util.TimeSeriesUtils import smooth, MetricType


def test_smooth():
    print(smooth(3, [ 0.878, 791.0, 376.0, 1.18, 0.959, 0.5], MetricType.HISTOGRAM))


def main(args):
    test_smooth()

if __name__ == "__main__":
    main(sys.argv)