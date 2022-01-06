# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import numpy as np

"""
Handles sequences where all values are the same.
In this case we apply a simple percentage threshold
to detect anomalies
"""


class ZeroDeviationClassifierTS(object):
    klassifier = {}
    #TODO values should be a 1d array
    def fit_transform(self, label, values, threshold, tolerance = 5):

        """

        :param label: key to identify the control dataset
        :param values: the control values
        :param threshold: boundary to detect anomalies

        """
        self.klassifier[label] = [np.nanmean(values, axis=0), threshold, tolerance]

    #TODO values should be a 1d array
    def predict(self, label, values, flag=0):
        """

        :param label: key to identify the control dataset
        :param values: the test values
        :param flag: 0 - allow lower values
                     1 - allow higher values
                     2 - allow exact values
        :return:
        """

        control_values =  self.klassifier[label][0]
        threshold = self.klassifier[label][1]
        tolerance = self.klassifier[label][2]
        score = 0.0
        predictions = np.array([1] * len(values))
        max_val = np.nanmax(control_values)
        for i, value in enumerate(values):
            anomaly = True
            if not np.isnan(value) and not np.isnan(control_values[i]):
                #TODO figure out tolerance
                # if max_val < 1.01 or abs(value - control_values[i]) > tolerance:
                #     anomaly = True
                # Higher is bad
                if flag == 0:
                    anomaly &= value - control_values[i] > threshold * control_values[i]
                # Lower is bad
                elif flag == 1:
                    anomaly &= control_values[i] - value > threshold * control_values[i]
                # Both are bad
                else:
                    anomaly &= abs(value - control_values[i]) > threshold * control_values[i]
            else:
                if np.isnan(value):
                    anomaly = False

            if anomaly:
                predictions[i] = -1
                score = score + 1
        return predictions, ((len(values) - score) / len(values))


#Uncomment to debug
#zdc = ZeroDeviationClassifierTS()
#zdc.fit_transform(1, np.array([[1, 7],
#                                [1, 7],
#                                [1, 7]]), 0.05)
# predictions, score = zdc.predict(1, np.array([[1, 13]]))
# print(predictions == -1)
# print(len(np.where(predictions == -1)[0]) == 1)
# print(predictions, score)
