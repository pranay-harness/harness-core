# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import hdbscan
import numpy as np
from sklearn.preprocessing import StandardScaler

"""
Uses Hierarchical density based clustering for n dimensional vectors as a means for classification
"""


class HDBScanClassifier(object):
    klassifier = {}

    def fit_transform(self, label, values):
        """

        :param label: key to identify dataset
        :param values: a numpy 2d array of shape m samples with n dimensions
        """
        self.klassifier[label] = np.array(values)

    def predict(self, label, values):

        """

        :param label: key to identify dataset
        :param values: a numpy 2d array of shape m samples with n dimensions for prediction
        :return: a predictions list of size m (one per input sample) with 1 for fit
                 and -1 for anomaly. Returns the percentage of anomalous values as the
                 score
        """
        control_data = self.klassifier[label]
        test_data = np.array(values)
        all_data = np.row_stack((control_data, test_data))

        clusterer = hdbscan.HDBSCAN(min_cluster_size=2, gen_min_span_tree=True, allow_single_cluster=True,
                                    metric='euclidean')
        clusterer.fit(StandardScaler().fit_transform(all_data))
        labels, probabilities = clusterer.labels_, clusterer.probabilities_

        control_probs, test_probs = probabilities[:len(control_data)], probabilities[len(control_data):]
        control_labels, test_labels = np.array(labels[:len(control_data)]), np.array(labels[len(control_data):])

        predictions = np.array([1] * len(test_data))
        for label in set(test_labels):
            # Falls outside the control clusters
            if label not in control_labels:
                predictions[test_labels == label] = -1
            else:
                # Falls inside a control cluster but may have a small probability or the control cluster
                # itself can be a small probability cluster
                test_low_probs = np.where(test_probs[test_labels == label] < 0.5)[0]
                control_high_prob = np.where(control_probs[control_labels == label] > 0.5)[0]
                if len(test_low_probs) > 0 or len(control_high_prob) == 0:
                    predictions[test_low_probs] = -1

        return predictions, float(len(values) - len(predictions[predictions == -1])) / len(values)







# control_data = [
#     [7., 1., 1., 1., 5., 16., 5., 3., 2.],
#     [12., 2., 1., 0., 1., 14., 9., 3., 2.],
#     [8., 0., 0., 0., 2., 11., 5., 5., 2.]]
#
# test_data = [[11., 0., 0., 0., 4., 0., 0., 8., 0.]]
#
# # knn = KNeighborsClassifier(3, metric=canberra, algorithm='braycurtis')
# # knn.fit(control_data, list(range(len(control_data))))
# # dist, neighbors = knn.kneighbors(control_data, return_distance=True)
# # print(neighbors)
# # print(pairwise_distances(control_data, metric=canberra))
#
# hdbscanKlassifier = HDBScanClassifier()
# hdbscanKlassifier.fit_transform(1, control_data)
#
# predictions, score = hdbscanKlassifier.predict(1, test_data)
#
# print(predictions, score)
