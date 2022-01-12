# Copyright 2017 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import math

import numpy as np


class ThreeSigmaClassifier(object):
    klassifier = {}
    tolerance = 5

    def fit_transform(self, label, values):
        vals = map(int, values[:, 1])
        np_values = np.array(vals)
        self.klassifier[label] = [np.mean(np_values, axis=0), np.std(np_values, axis=0)]

    def predict(self, label, values, flag=0):
        mean = self.klassifier[label][0]
        std = self.klassifier[label][1]
        vals = map(int, values[:, 1])
        score = 0.0
        predictions = np.array([1] * len(vals))
        for i, value in enumerate(vals):
            anomaly = False
            if math.floor(abs(value - mean)) > self.tolerance:
                anomaly = True
            if flag == 0:
                anomaly &= math.floor(value - mean) > math.ceil(5 * std)
            elif flag == 1:
                anomaly &= math.floor(value - mean) < math.ceil(5 * std)
            else:
                anomaly &= math.floor(abs(value - mean)) > math.ceil(5 * std)

            if anomaly:
                predictions[i] = -1
                score = score + 1
        return predictions, ((len(values) - score) / len(values))


# zdc = ThreeSigmaClassifier()
# zdc.fit_transform(1, np.array([[1, 65],
#                                    [1, 55],
#                                    [1, 63]]
#                                    ))
# predictions, score = zdc.predict(1, np.array([[1, 74], [1,76], [1,77]]))
# print(predictions, score)
