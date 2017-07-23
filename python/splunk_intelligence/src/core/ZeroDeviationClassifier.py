import numpy as np
import math

"""
Handles sequences where all values are the same.
In this case we apply a simple percentage threshold
to detect anomalies
"""


class ZeroDeviationClassifier(object):
    klassifier = {}
    tolerance = 5
    #TODO values should be a 1d array
    def fit_transform(self, label, values, threshold):

        """

        :param label: key to identify the control dataset
        :param values: the control values
        :param threshold: boundary to detect anomalies

        """
        vals = map(int, values[:, 1])
        np_values = np.array(vals)
        self.klassifier[label] = [np.mean(np_values, axis=0), threshold]

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
        mean = self.klassifier[label][0]
        threshold = self.klassifier[label][1]
        vals = map(int, values[:, 1])
        score = 0.0
        predictions = np.array([1] * len(vals))
        for i, value in enumerate(vals):
            anomaly = False
            if math.floor(abs(value - mean)) > self.tolerance:
                anomaly = True
            # Higher is bad
            if flag == 0:
                anomaly &= math.floor(value - mean)  > math.ceil(threshold * mean)
            # Lower is bad
            elif flag == 1:
                anomaly &= math.floor(value - mean) < math.ceil(threshold * mean)
            # Both are bad
            else:
                anomaly &= math.floor(abs(value - mean)) > math.ceil(threshold * mean)

            if anomaly:
                predictions[i] = -1
                score = score + 1
        return predictions, ((len(values) - score) / len(values))


#Uncomment to debug
# zdc = ZeroDeviationClassifier()
# zdc.fit_transform(1, np.array([[1, 7],
#                                [1, 7],
#                                [1, 7]]), 0.05)
# predictions, score = zdc.predict(1, np.array([[1, 13]]))
# print(predictions == -1)
# print(len(np.where(predictions == -1)[0]) == 1)
# print(predictions, score)
