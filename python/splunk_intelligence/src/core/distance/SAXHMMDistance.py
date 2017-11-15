import numpy as np
import pandas as pd

from core.data_structures.SuffixTree import SuffixTree
from core.distance.LevenShtein import LevenShteinDistance
from core.util.TimeSeriesUtils import smooth, MetricToDeviationType, normalize_metric


class SAXHMMDistance(object):
    cutpoints = {3: [-np.inf, -0.43, 0.43, np.inf],
                 4: [-np.inf, -0.67, 0, 0.67, np.inf],
                 5: [-np.inf, -0.84, -0.25, 0.25, 0.84, np.inf],
                 6: [-np.inf, -0.97, -0.43, 0, 0.43, 0.97, np.inf],
                 7: [-np.inf, -1.07, -0.57, -0.18, 0.18, 0.57, 1.07, np.inf],
                 8: [-np.inf, -1.15, -0.67, -0.32, 0, 0.32, 0.67, 1.15, np.inf]
                 }
    alphabets = ['a', 'b', 'c', 'd', 'e', 'f', 'g']
    # self.dist = [[0, 0, 0.67, 1.34], [0, 0, 0, 0.67], [0.67, 0, 0, 0], [1.34, 0.67, 0, 0]]
    inverted_alphabet = dict([(w, i) for (i, w) in enumerate(alphabets, 1)])
    inverted_alphabet['x'] = len(inverted_alphabet) + 1

    @staticmethod
    def create_dist_matrix(alphabets, cutpoints):
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
                    # cutpoints is indexed from 1
                    dist[j] = 0 if abs(j - i) <= 1 else cutpoints[no_of_alphabets][max(i + 1, j + 1) - 1] \
                                                        - cutpoints[no_of_alphabets][min(i + 1, j + 1)]
                    if dist[j] > max_dist:
                        max_dist = dist[j]
            dist_matrix.append(dist)

        dist_matrix[rows - 1][cols - 1] = 0.0
        dist_matrix = np.asarray(dist_matrix)
        dist_matrix[~np.isfinite(dist_matrix)] = max_dist + 0.1
        return dist_matrix

    @staticmethod
    def create_threshold(distance_matrix):
        thresholds = {0: 0.01}
        n = len(distance_matrix) - 1
        for tolerance in range(1, 4):
            threshold = 0.0
            max_deviation = 1 + tolerance
            for i in range(n):
                ind = (i + max_deviation) if (i + max_deviation) < n else i - max_deviation
                if ind == -1:
                    continue
                threshold = max(distance_matrix[i][ind], threshold)
            thresholds[tolerance] = threshold
        return thresholds

    @staticmethod
    def get_adjusted_distance(metric_deviation_type, min_threshold, apply_sax, x, y, a, b):
        if SAXHMMDistance.low_deviation(metric_deviation_type, min_threshold, x, y):
            return 0
        if SAXHMMDistance.high_deviation(metric_deviation_type, x, y):
            return 2
        if apply_sax:
            return SAXHMMDistance.get_bucket_dist(metric_deviation_type, SAXHMMDistance.inverted_alphabet[a],
                                                  SAXHMMDistance.inverted_alphabet[b])
        else:
            return SAXHMMDistance.get_fixed_distance(x, y)

    @staticmethod
    def high_deviation(metric_deviation_type, x, y):
        if not np.isnan(x) and not np.isnan(y):
            # Higher is bad
            if metric_deviation_type == MetricToDeviationType.HIGHER:
                return y > 10 * x if x != 0 else y > 10
            # Lower is bad
            elif metric_deviation_type == MetricToDeviationType.LOWER:
                return x > 10 * y if y != 0 else x > 10
            # Both are bad
            else:
                return SAXHMMDistance.high_deviation(MetricToDeviationType.LOWER, x, y) or \
                       SAXHMMDistance.high_deviation(MetricToDeviationType.HIGHER, x, y)

        else:
            return False

    @staticmethod
    def low_deviation(metric_deviation_type, min_threshold, x, y):
        if not np.isnan(x) and not np.isnan(y):
            return abs(y - x) < min_threshold or abs(y - x) < 0.5 * min(x, y)
        else:
            return False

    @staticmethod
    def get_bucket_dist(metric_deviation_type, iw1, iw2):
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
        if np.isnan(y):
            return 0
        elif np.isnan(x):
            return 2.24
        else:
            return 0

    @staticmethod
    def detect_pattern_anomaly(stc, stt, pattern):
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
                        for i in range(1, len(pattern) - w + 1):
                            prob = prob / stc.get_counts(pattern[i:i + w - 1])
                        return stt.get_counts(pattern) - prob
            return 2

    distance_matrix = create_dist_matrix.__func__(alphabets, cutpoints)
    thresholds = create_threshold.__func__(distance_matrix)


class SAXHMMDistanceFinder(object):
    def __init__(self, metric_name, smooth_window, tolerance, control_data_dict, test_data_dict, metric_deviation_type,
                 min_metric_threshold):
        self.metric_name = metric_name
        self.smooth_window = smooth_window
        self.thresholds = SAXHMMDistance.thresholds
        self.tolerance = tolerance
        self.metric_deviation_type = metric_deviation_type
        self.min_metric_threshold = min_metric_threshold
        self.control_data_dict = control_data_dict
        self.test_data_dict = test_data_dict
        self.apply_sax = True
        self.ld = LevenShteinDistance(self.get_dist_index_letter)
        self.control_cuts, self.test_cuts, self.apply_sax \
            = self.get_cuts(metric_name, control_data_dict['data'], test_data_dict['data'])
        self.a_control_data = []
        self.a_test_data = []

    def get_dist_index_letter(self, control_i, test_i, control_letter, test_letter):
        return self.get_dist_value_letter(self.a_control_data[control_i],
                                          self.a_test_data[test_i],
                                          control_letter,
                                          test_letter)

    def get_dist_value_letter(self, control_val, test_val, control_letter, test_letter):
        return SAXHMMDistance.get_adjusted_distance(self.metric_deviation_type,
                                                    self.min_metric_threshold,
                                                    self.apply_sax,
                                                    control_val, test_val,
                                                    control_letter,
                                                    test_letter)

    def transform_fixed_cut(self, w, metric_values):
        '''

        Discretize time series using the window size and the alphabet set for each node

        :param w: discretization moving window. Values in each window is mapped to a single value
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
            acut[~pd.isnull(acut)] = 'q'
            acut[pd.isnull(acut)] = 'x'
            cuts.append(acut)
        return np.array(cuts)

    def transform_metric(self, w, metric_values):
        '''

        Discretize time series using the window size and the alphabet set for each node

        :param w: discretization moving window. Values in each window is mapped to a single value
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

        # normalize data
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
        # Miniscule deviation. Use rule based distances
        else:
            apply_sax = False
            control_cuts = self.transform_fixed_cut(1, control_data_norm_smoothed)
            test_cuts = self.transform_fixed_cut(1, test_data_norm_smoothed)

        return control_cuts, test_cuts, apply_sax

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
            # Assign empty arrays so
            if buckets == 0:
                continue
            max_dist = +np.inf
            stt = SuffixTree()
            stt.build_naive(''.join(a_test_cut))
            for j, a_control_cut in enumerate(self.control_cuts):

                a_test_cut_str = ''.join(a_test_cut)
                dist_vector = np.asarray(
                    [self.get_dist_index_letter(z, z, a, b)
                     for z, (a, b) in enumerate(zip(a_control_cut, a_test_cut))])

                a_optimal_test_cut = ''
                a_optimal_test_indices = []
                if np.nansum(dist_vector) > 0:
                    a_optimal_test_cut, a_optimal_test_indices = list(self.ld.find_optimal_alignment(a_control_cut,
                                                                                                     a_test_cut,
                                                                                                     ))
                    dist_vector = np.asarray(
                        [self.get_dist_value_letter(control_data_smoothed[j][z],
                                                    np.nan if a_optimal_test_indices[z] == -1 else
                                                    test_data_smoothed[i][a_optimal_test_indices[z]], a, b)
                         for z, (a, b) in
                         enumerate(zip(a_control_cut,
                                       a_optimal_test_cut))])

                    dist_vector = np.asarray([dist if dist == 0.0 or a_control_cut[k] != 'x'
                                              else dist if abs(SAXHMMDistance.detect_pattern_anomaly(stc_list[j], stt,
                                                                                                     a_test_cut_str[
                                                                                                     :3] if k == 0
                                                                                                     else a_test_cut_str[
                                                                                                          -3:] if k == len(
                                                                                                         dist_vector) - 1
                                                                                                     else a_test_cut_str[
                                                                                                          k - 1:k + 2])) > 1
                    else 0
                                              for (k, dist) in enumerate(dist_vector)])

                ma = np.ma.masked_array(dist_vector, np.isnan(dist_vector))
                ma0 = ma.filled(0)

                # Replace distance of 2.24 with tolerance
                dist_vector[dist_vector == 2.24] = self.thresholds[self.tolerance]

                if 'weights' in self.test_data_dict:
                    control_weights_smoothed = [
                        smooth(self.smooth_window, a_weight, self.control_data_dict['weights_type'])
                        for a_weight in self.control_data_dict['weights']]

                    test_weights_smoothed = [
                        smooth(self.smooth_window, a_weight, self.control_data_dict['weights_type'])
                        for a_weight in self.test_data_dict['weights']]

                    wt = [np.nan if np.isnan(w) else w * v
                          for (p, (w, v)) in
                          enumerate(zip(test_weights_smoothed[i], test_data_smoothed[i]))]
                    wc = [np.nan if np.isnan(w) else w * v
                          for (p, (w, v)) in
                          enumerate(zip(control_weights_smoothed[j], control_data_smoothed[j]))]

                    if len(a_optimal_test_indices) == 0:
                        weights = np.array([1 if np.isnan(u) or np.isnan(v) else u / v for (u, v) in zip(wt, wc)])
                    else:
                        weights = np.array([1 if u == -1 or np.isnan(v) else 1 if wt[u] > v else
                        wt[u] / v for (u, v)
                                            in zip(a_optimal_test_indices, wc)])

                    w_dist = np.sum(
                        [((p + 1) * v * w) for (p, (v, w)) in
                         enumerate(zip(dist_vector[~np.isnan(test_data_smoothed[i])],
                                       weights[~np.isnan(test_data_smoothed[i])]))])

                else:
                    w_dist = np.sum([((p + 1) * v) for (p, v)
                                     in enumerate(dist_vector[~np.isnan(test_data_smoothed[i])])])

                w_dist /= ((buckets * (buckets + 1)) / 2) * 1.0
                adist = len(ma0[ma0 != 0]) * w_dist
                if adist < max_dist:
                    control_values[i] = control_data_smoothed[j]
                    max_dist = adist
                    distances[i] = ma0
                    nn[i] = j
                    score[i] = w_dist
                    cuts[i] = a_control_cut
                    optimal_cuts[i] = a_optimal_test_cut
                    optimal_data[i] = np.array([]) if len(a_optimal_test_indices) == 0 else \
                        np.array([0 if q == -1 else test_data_smoothed[i][q] for q in a_optimal_test_indices])
                    risk[i] = 0 if score[i] < self.thresholds.get(self.tolerance - 1) else 1 \
                        if score[i] < self.thresholds.get(self.tolerance) \
                        else 2

        return dict(distances=distances, nn=nn, score=score, control_cuts=cuts, test_cuts=self.test_cuts,
                    optimal_test_cuts=optimal_cuts, optimal_test_data=optimal_data, risk=risk,
                    control_values=control_values, test_values=test_values)
