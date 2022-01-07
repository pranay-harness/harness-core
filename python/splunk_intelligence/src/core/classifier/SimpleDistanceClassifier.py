# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import numpy as np
import scipy.spatial.distance as dist
import sys
from core.util.lelogging import get_log

logger = get_log(__name__)

'''
Uses the "braycurtis" distance along with a threshold to predict anomalies.
'''


class SimpleDistanceClassifier(object):
    klassifier = {}

    def fit_transform(self, label, values, threshold):
        """

        :param label:  key to identify the dataset
        :param values: a numpy 2d array of shape m samples with n dimensions
        :param threshold: the distance threshold beyond which the point is classified
                          as an anomaly. The threshold should be set low to about 0.1
                          Reduce it further for tighter predictions
        """
        self.klassifier[label] = [np.array(values), threshold]

    def predict(self, label, values, flag=0):
        """

        :param label: key to identify the dataset
        :param values: a numpy 2d array of shape m samples with n dimensions for prediction
        :param flag: "0" implies lower values are better when compared to control
                     "1" implies higher values are better when compared to control
                     "anything else" implies values closer to control are preferred.
                     i.e lower or higher both are bad.
        :return: a predictions list of size m (one per input sample) with 1 for fit
                 and -1 for anomaly. Returns the percentage of anomalous values as the
                 score
        """

        baseline = self.klassifier[label][0]
        test = np.array(values)
        threshold = self.klassifier[label][1]
        assert(baseline.shape[1] == test.shape[1])

        predictions = np.array([-1] * len(values))
        for i in range(test.shape[0]):
            for j in range(baseline.shape[0]):
                if self.dist(baseline[j,], test[i,]) < threshold:
                    predictions[i] = 1
                    break
        return predictions, float((len(values) - len(predictions[predictions == -1]))) / len(values)

    def dist(self, x, y, flag=0):
        if x.shape != y.shape:
            logger.error("Shape of x and y doesn't match", x, y)
            sys.exit(-1)
        # Higher is bad
        if flag == 0:
            for i in range(x.shape[0]):
                if x[i] > y[i]:
                    y[i] = x[i]
        # Lower is bad
        elif flag == 1:
            for i in range(x.shape[0]):
                if x[i] < y[i]:
                    y[i] = x[i]

        return dist.pdist(np.row_stack((x, y)), metric='braycurtis')

# spc = SimpleDistanceClassifier()
# spc.fit_transform(1, np.array([[7., 1., 1., 1., 5., 16., 5., 3., 2],
#                                [8., 0., 0., 0., 2., 11., 5., 5., 2],
#                                [11., 0., 0., 0., 4., 0., 0., 8., 0]]), 0.1)
# print(spc.predict(1, np.array([[12., 2., 1., 0., 1., 14., 9., 3., 2]])))

# spc = SimpleDistanceClassifier()
# print(spc.dist(np.array([7., 1., 1., 1., 5., 16., 5., 3., 2.]),
#     np.array([20., 0., 0., 10., 2., 11., 5., 5., 2])))
