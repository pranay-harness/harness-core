import numpy as np
import pandas as pd

from core.data_structures.SuffixTree import SuffixTree
from core.distance.LevenShtein import LevenShteinDistance
from core.util.TimeSeriesUtils import smooth, MetricToDeviationType, normalize_metric, ThresholdComparisonType


class SAXHMMDistance(object):
    """
    Defines the methods to find the distances between 2 discretized comparison
    units
    """
    # Partitions a Gaussian distribution into equally probable buckets
    cutpoints = {3: [-np.inf, -0.43, 0.43, np.inf],
                 4: [-np.inf, -0.67, 0, 0.67, np.inf],
                 5: [-np.inf, -0.84, -0.25, 0.25, 0.84, np.inf],
                 6: [-np.inf, -0.97, -0.43, 0, 0.43, 0.97, np.inf],
                 7: [-np.inf, -1.07, -0.57, -0.18, 0.18, 0.57, 1.07, np.inf],
                 8: [-np.inf, -1.15, -0.67, -0.32, 0, 0.32, 0.67, 1.15, np.inf]
                 }
    # number of discretized states
    alphabets = ['a', 'b', 'c', 'd', 'e', 'f', 'g']
    # self.dist = [[0, 0, 0.67, 1.34], [0, 0, 0, 0.67], [0.67, 0, 0, 0], [1.34, 0.67, 0, 0]]
    inverted_alphabet = dict([(w, i) for (i, w) in enumerate(alphabets, 1)])
    # add alphabet X to represent gaps
    inverted_alphabet['x'] = len(inverted_alphabet) + 1

    @staticmethod
    def create_dist_matrix(alphabets, cutpoints):
        """
        Create distance matrix between any 2 letters from the alphabet.
        The rows are from a to g, and the cols are from a to g.
        so D(0,0) = D(a,a) = 0
        D(p, X) = D(x,p) = maximum for p in [a,b,c,d,e,f,g]
        D(X,X) = 0
        """
        no_of_alphabets = len(alphabets)
        # add an extra row for alphabet 'x'
        # which indicates no data
        cols = rows = no_of_alphabets + 1
        dist_matrix = []
        max_dist = -np.inf
        for i in range(0, rows):
            dist = [0] * cols
            for j in range(0, cols):
                if rows - 1 == i:
                    dist[j] = np.nan
                elif cols - 1 == j:
                    dist[j] = np.nan
                else:
                    # cutpoints are indexed from 1
                    dist[j] = 0 if abs(j - i) == 0 else cutpoints[no_of_alphabets][max(i + 1, j + 1) - 1] \
                                                        - cutpoints[no_of_alphabets][min(i + 1, j + 1)]
                    if dist[j] > max_dist:
                        max_dist = dist[j]
            dist_matrix.append(dist)

        dist_matrix[rows - 1][cols - 1] = 0.0
        dist_matrix = np.asarray(dist_matrix)
        max_dist = max_dist + 0.1
        dist_matrix[~np.isfinite(dist_matrix)] = max_dist
        return dist_matrix, max_dist

    @staticmethod
    def create_threshold(distance_matrix):
        """
         calculate allowable distances between any 2 comparison units based on tolerance
         1 => allows 1 standard deviations (low)
         2 => allows 2 standard deviations (med)
         3 => allows 3 standard deviations (high)
         4 => allows 4 standard deviations

        """
        thresholds = {}
        n = len(distance_matrix) - 1
        for tolerance in range(1, 6):
            threshold = 0.0
            max_deviation = tolerance
            for i in range(n):
                ind = (i + max_deviation) if (i + max_deviation) < n else i - max_deviation
                if ind == -1:
                    continue
                threshold = max(distance_matrix[i][ind], threshold)
            thresholds[tolerance] = threshold
        # tolerance of 1 will always have a threshold of 0
        # setting threshold for tolerance of 1 to 75% of tolerance of 2
        thresholds[1] = 0.75 * thresholds[2]
        return thresholds

    @staticmethod
    def get_adjusted_distance(metric_deviation_type, min_threshold_delta, min_threshold_ratio, abs_dev_range,
                              apply_sax, x, y, a, b):
        """

        The distance is adjusted based on high and low deviation notions, to safe guard against
        control and test belonging to different distributions, and to apply minimum thresholds based
        on domain knowledge. It also uses user defined custom thresholds as override

        :param metric_deviation_type:
        :param min_threshold_delta: absolute difference should be greater than this
        :param min_threshold_ratio: % change should be greate than this
        :param apply_sax: if true use letter distance else fixed distance
        :param x: the x value
        :param y: the y value
        :param a: the letter from the Alphabet for the x value
        :param b: the letter from the Alphabet for the y value
        :return: the adjusted distance
        """
        if SAXHMMDistance.user_defined_override(metric_deviation_type, abs_dev_range[0], abs_dev_range[1], y):
            return 0
        if SAXHMMDistance.low_deviation(metric_deviation_type, min_threshold_delta, min_threshold_ratio, x, y):
            return 0
        if SAXHMMDistance.high_deviation(metric_deviation_type, x, y):
            return SAXHMMDistance.max_dist
        if apply_sax:
            return SAXHMMDistance.get_letter_dist(metric_deviation_type, SAXHMMDistance.inverted_alphabet[a],
                                                  SAXHMMDistance.inverted_alphabet[b])
        else:
            return SAXHMMDistance.get_fixed_distance(x, y)

    @staticmethod
    def high_deviation(metric_deviation_type, x, y):
        """
          We treat the data for a metric as belonging to a Gaussian distribution. It can happen that
          the control and test data does not belong to the same distribution.
          For instance, the difference between them can be very large. This algorithm
          does not work well, if control and test don't belong to the same distribution.
          So we check if one value is 10x the other to safeguard against this.
        """
        if not np.isnan(x) and not np.isnan(y):
            # Higher is bad
            if metric_deviation_type == MetricToDeviationType.HIGHER:
                return y > 10 * x and x != 0
            # Lower is bad
            elif metric_deviation_type == MetricToDeviationType.LOWER:
                return x > 10 * y and y != 0
            # Both are bad
            else:
                return SAXHMMDistance.high_deviation(MetricToDeviationType.LOWER, x, y) or \
                       SAXHMMDistance.high_deviation(MetricToDeviationType.HIGHER, x, y)

        else:
            return False

    @staticmethod
    def user_defined_override(metric_deviation_type, min_threshold, max_threshold, y):
        if not np.isnan(y):
            if metric_deviation_type == MetricToDeviationType.HIGHER:
                return y < max_threshold
            # Lower is bad
            elif metric_deviation_type == MetricToDeviationType.LOWER:
                return y > min_threshold
            # Both are bad
            else:
                return min_threshold < y < max_threshold
        else:
            return False

    @staticmethod
    def low_deviation(metric_deviation_type, min_threshold_delta, min_threshold_ratio, x, y):
        """
            Apply the distribution to compute distance only if the difference is above a specified
            minimum threshold. This information is derived from domain knowledge.
            For instance, if the difference in average response time is less than
            50 ms, then it is acceptable regardless of what the distribution tells us.

            Also, its low deviation if the % change is within a specified value. For now its at 50%
        """
        if not np.isnan(x) and not np.isnan(y):
            return abs(y - x) < min_threshold_delta or (min(x, y) > 0 and abs(y - x) < min_threshold_ratio * min(x, y))
        else:
            return False

    @staticmethod
    def get_letter_dist(metric_deviation_type, iw1, iw2):
        """
        Get the distance between 2 letters of the alphabet based on the metric_deviation_type:
         
         if metric_deviation_type == MetricToDeviationType.HIGHER, then higher deviations are bad, i.e 
         the second alphabet should be higher than the first
         
         if metric_deviation_type == MetricToDeviationType.LOWER, then lower deviations are bad, i.e 
         the first alphabet should be lower than the second
         
        """
        if metric_deviation_type == MetricToDeviationType.HIGHER:
            return 0 if (iw2 <= iw1 != SAXHMMDistance.inverted_alphabet['x']) or iw2 == \
                                                                                 SAXHMMDistance.inverted_alphabet['x'] \
                else SAXHMMDistance.distance_matrix[iw1 - 1][iw2 - 1]
        elif metric_deviation_type == MetricToDeviationType.LOWER:
            return 0 if iw2 >= iw1 or iw2 == SAXHMMDistance.inverted_alphabet['x'] \
                else SAXHMMDistance.distance_matrix[iw1 - 1][iw2 - 1]
        else:
            return 0 if iw2 == SAXHMMDistance.inverted_alphabet['x'] else SAXHMMDistance.distance_matrix[iw1 - 1][
                iw2 - 1]

    @staticmethod
    def get_fixed_distance(x, y):
        """
            This is used when the deviations are so small that the Gaussian distribution is not applied.

            D(p,x) = 0 for p in [a,b,c,d,e,f,g]
            D(x,p) = 2.24 for p in [a,b,c,d,e,f,g]
            all other D(i,j) = 0 for i,j in [a,b,c,d,e,f,g]
        """
        if np.isnan(y):
            return 0
        elif np.isnan(x):
            return SAXHMMDistance.max_dist
        else:
            return 0

    @staticmethod
    def detect_pattern_anomaly(stc, stt, pattern):
        """
        Use the HMM model to predict the likelihood
        of the pattern occurring in the control data.

        returns a positive integer from 0 to inf.
        The higher the value the higher the probability that the pattern is an anomaly.
        """
        count = stc.get_counts(pattern)
        if count > 0:
            return stt.get_counts(pattern) - count
        else:
            w = len(pattern) - 1
            while w > 1:
                prob = 1.0
                for i in range(len(pattern) - w + 1):
                    count = float(stc.get_counts(pattern[i:i + w]))
                    if count == 0:
                        w -= 1
                        break
                    prob *= count
                    if i == len(pattern) - w:
                        for j in range(1, len(pattern) - w + 1):
                            prob = prob / stc.get_counts(pattern[j:j + w - 1])
                        return stt.get_counts(pattern) - prob
            return +np.inf

    distance_matrix, max_dist = create_dist_matrix.__func__(alphabets, cutpoints)
    thresholds = create_threshold.__func__(distance_matrix)


class SAXHMMDistanceFinder(object):
    def __init__(self, transaction_name, metric_name, smooth_window, tolerance, control_data_dict, test_data_dict, metric_template,
                 comparison_unit_window):
        self.transaction_name = transaction_name
        self.metric_name = metric_name
        self.smooth_window = smooth_window
        self.thresholds = SAXHMMDistance.thresholds
        self.tolerance = tolerance
        self.metric_template = metric_template
        self.comparison_unit_window = comparison_unit_window
        self.control_data_dict = control_data_dict
        self.test_data_dict = test_data_dict
        self.apply_sax = True
        self.ld = LevenShteinDistance(self.get_dist_index_letter)
        self.control_cuts, self.test_cuts, self.apply_sax \
            = self.get_cuts(metric_name, control_data_dict['data'], test_data_dict['data'])
        self.a_control_data = []
        self.a_test_data = []

    def get_dist_index_letter(self, control_i, test_i, control_letter, test_letter):
        """
        Returns the adjusted distance between a control and test value

        :param control_i: the index of the control value
        :param test_i: the index of the test value
        :param control_letter: the letter for the value from the alphabet
        :param test_letter: the letter for the value from the alphabet
        :return:
        """
        return self.get_dist_value_letter(self.a_control_data[control_i],
                                          self.a_test_data[test_i],
                                          control_letter,
                                          test_letter)

    def get_dist_value_letter(self, control_val, test_val, control_letter, test_letter):
        """
        Returns the adjusted distance between a control and test value

        :param control_val: the control value
        :param test_val: the test value
        :param control_letter: the  letter for the value from the alphabet
        :param test_letter: the  letter for the value from the alphabet
        :return:
        """
        return SAXHMMDistance.get_adjusted_distance(self.metric_template.get_deviation_type(self.metric_name),
                                                    self.metric_template.get_deviation_threshold(self.metric_name,
                                                                                                 ThresholdComparisonType.DELTA),
                                                    self.metric_template.get_deviation_threshold(self.metric_name,
                                                                                                 ThresholdComparisonType.RATIO),
                                                    self.metric_template.get_abs_deviation_range(self.transaction_name, self.metric_name),
                                                    self.apply_sax,
                                                    control_val, test_val,
                                                    control_letter,
                                                    test_letter)

    def transform_fixed_cut(self, w, metric_values):
        """

        Discretize time series using the window size and the alphabet set for each node. The finite values
        are mapped to a fixed letter q or to x if the value is not finite.

        :param w: moving window. AVERAGE Values in each window and map the averaged value to a letter
        from the alphabet
        :param metric_values: 2d array_like observations for control or test group
                                For a 2 node control group it will look something like below
                                [[150, 134, 125] , [123, 345, 432]
        :return: the discretized alphabet representation for the time series
        """
        cuts = []
        for metric_values_host in metric_values:
            n = len(metric_values_host)
            l = np.array_split(metric_values_host, n / w)
            means = [np.nanmean(z) for z in l]
            cutpoint = SAXHMMDistance.cutpoints.get(len(SAXHMMDistance.alphabets))
            acut = np.array(pd.cut(means, bins=cutpoint, labels=SAXHMMDistance.alphabets))
            acut[~pd.isnull(acut)] = 'q'
            acut[pd.isnull(acut)] = 'x'
            cuts.append(acut)
        return np.array(cuts)

    def transform_metric(self, w, metric_values):
        '''

        Discretize time series using the window size and the alphabet set for each node. Non finite
        values are mapped to the letter x.

        :param w: moving window. AVERAGE Values in each window and map the averaged value to a letter
        from the alphabet
        :param metric_values: 2d array_like observations for control or test group
                                For a 2 node control group it will look something like below
                                [[150, 134, 125] , [123, 345, 432]
        :return: the discretized alphabet representation for the time series
        '''
        cuts = []
        for metric_values_host in metric_values:
            n = len(metric_values_host)
            l = np.array_split(metric_values_host, n / w)
            means = [np.nanmean(z) for z in l]
            cutpoint = SAXHMMDistance.cutpoints.get(len(SAXHMMDistance.alphabets))
            acut = np.array(pd.cut(means, bins=cutpoint, labels=SAXHMMDistance.alphabets))
            acut[pd.isnull(acut)] = 'x'
            cuts.append(acut)
        return np.array(cuts)

    def get_cuts(self, metric_name, control_data, test_data):
        """
        Transform the time series to a discretized alphabet representation.
        """

        # normalize data.
        mean, std, control_data_norm, test_data_norm = \
            normalize_metric(np.array(control_data), np.array(test_data))

        # smooth data
        control_data_norm_smoothed = np.asarray(
            [smooth(self.smooth_window, data, metric_name) for data in control_data_norm])
        test_data_norm_smoothed = np.asarray([smooth(self.smooth_window, data, metric_name) for data in test_data_norm])

        # If sufficient deviation, use dicretization
        # based on a normal distribution
        if std != 0.0 and std / mean > 0.01:
            apply_sax = True
            control_cuts = self.transform_metric(1, control_data_norm_smoothed)
            test_cuts = self.transform_metric(1, test_data_norm_smoothed)
        # Miniscule deviation. Use fixed cuts and distances
        else:
            apply_sax = False
            control_cuts = self.transform_fixed_cut(1, control_data_norm_smoothed)
            test_cuts = self.transform_fixed_cut(1, test_data_norm_smoothed)

        return control_cuts, test_cuts, apply_sax

    def get_distance(self, control_cut, test_cut, control_data, test_data):
        dist_vector = []
        w = self.comparison_unit_window
        n = len(test_cut)
        new_test_cut = []
        for i in range(0, n, w):
            control_sub_cut = [(b[0], b[1]) for b in sorted(enumerate(control_cut[i:i+w]), key=lambda i:i[1])]
            test_sub_cut = [(b[0], b[1]) for b in sorted(enumerate(test_cut[i:i+w]), key=lambda i:i[1])]

            dist_vector.extend(np.asarray(
                [self.get_dist_value_letter(control_data[a[0] + i], test_data[b[0] + i], a[1], b[1])
                 for (a, b) in zip(control_sub_cut, test_sub_cut)]))
            new_test_cut.extend([x[1] for x in test_sub_cut])
        return np.asarray(dist_vector), np.asarray(new_test_cut)

    def compute_dist(self):

        control_data_smoothed = [smooth(self.smooth_window, a_data, self.control_data_dict['data_type'])
                                 for a_data in self.control_data_dict['data']]
        test_data_smoothed = [smooth(self.smooth_window, a_data, self.test_data_dict['data_type'])
                              for a_data in self.test_data_dict['data']]

        control_values = [np.array([])] * len(self.test_cuts)
        test_values = [np.array([])] * len(self.test_cuts)
        distances = [np.array([])] * len(self.test_cuts)
        optimal_cuts = [''] * len(self.test_cuts)
        optimal_data = [np.array([])] * len(self.test_cuts)
        nn = [-1] * len(self.test_cuts)
        cuts = [np.array([])] * len(self.test_cuts)
        score = [-1] * len(self.test_cuts)
        risk = [0] * len(self.test_cuts)

        # Build the suffix trees with the frequency counts
        # for the control data set
        stc_list = []
        for j, a_control_cut in enumerate(self.control_cuts):
            stc = SuffixTree()
            stc.build_naive(''.join(a_control_cut))
            stc_list.append(stc)
            self.a_control_data = control_data_smoothed[j]

        for i, a_test_cut in enumerate(self.test_cuts):
            self.a_test_data = test_data_smoothed[i]
            test_values[i] = test_data_smoothed[i]
            buckets = len(test_data_smoothed[i][~np.isnan(test_data_smoothed[i])])
            # All test values are NaN
            if buckets == 0:
                continue
            max_dist = +np.inf
            stt = SuffixTree()
            stt.build_naive(''.join(a_test_cut))
            for j, a_control_cut in enumerate(self.control_cuts):

                a_test_cut_str = ''.join(a_test_cut)
                dist_vector, new_test_cut = self.get_distance(a_control_cut, a_test_cut, control_data_smoothed[j],
                                                test_data_smoothed[i])

                a_optimal_test_cut = ''
                a_optimal_test_indices = []
                if np.nansum(dist_vector) > 0:
                    a_optimal_test_cut, a_optimal_test_indices = list(self.ld.find_optimal_alignment(a_control_cut,
                                                                                                     a_test_cut,
                                                                                                     ))
                    a_optimal_test_data = [np.nan if z == -1 else
                                           test_data_smoothed[i][z] for z in
                                           a_optimal_test_indices]

                    dist_vector, new_test_cut = self.get_distance(a_control_cut, a_optimal_test_cut, control_data_smoothed[j],
                                                    a_optimal_test_data)

                    dist_vector = np.asarray([dist if dist == 0.0 or a_control_cut[k] != 'x'
                                              else dist if abs(SAXHMMDistance.detect_pattern_anomaly(stc_list[j], stt,
                                                                                                     a_test_cut_str[
                                                                                                     :3] if k == 0
                                                              else a_test_cut_str[-3:] if k == len(dist_vector) - 1
                                                              else a_test_cut_str[k - 1:k + 2])) > 1
                                              else 0
                                              for (k, dist) in enumerate(dist_vector)])

                ma = np.ma.masked_array(dist_vector, np.isnan(dist_vector))
                ma0 = ma.filled(0)

                # max_dist is a special marker.
                dist_vector[dist_vector == SAXHMMDistance.max_dist] = self.thresholds[self.tolerance + 1] + 0.001

                # Time weight the distance scores. More recent time stamps are weighted higher.
                w_dist = np.sum(
                    [((p + 1) * v) for (p, v) in enumerate(dist_vector[new_test_cut != 'x'])])

                w_dist /= ((buckets * (buckets + 1)) / 2) * 1.0

                # same distance score but across more buckets are weighted higher.
                # This is only used to find the nearest neighbour.
                adist = len(ma0[ma0 != 0]) * w_dist
                if adist < max_dist:
                    control_values[i] = control_data_smoothed[j]
                    max_dist = adist
                    distances[i] = ma0
                    nn[i] = j
                    score[i] = w_dist
                    cuts[i] = a_control_cut
                    optimal_cuts[i] = a_optimal_test_cut
                    optimal_data[i] = np.array([]) if len(a_optimal_test_indices) == 0 else np.array(
                        [0 if q == -1 else test_data_smoothed[i][q] for q in a_optimal_test_indices])
                    risk[i] = \
                        0 if score[i] <= self.thresholds.get(self.tolerance) \
                            else 1 if score[i] <= self.thresholds.get(self.tolerance + 1) else 2
                    if score[i] == 0:
                        break

        return dict(distances=distances, nn=nn, score=score, control_cuts=cuts, test_cuts=self.test_cuts,
                    optimal_test_cuts=optimal_cuts, optimal_test_data=optimal_data, risk=risk,
                    control_values=control_values, test_values=test_values)