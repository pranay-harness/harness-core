import numpy as np


class ThreeSigmaClassifier(object):
    klassifier = {}

    def fit_transform(self, label, values):
        vals = map(int, values[:, 1])
        np_values = np.array(vals)
        self.klassifier[label] = [np.mean(np_values, axis=0), np.std(np_values, axis=0)]

    def predict(self, label, values, flag=0):
        mean = self.klassifier[label][0]
        std = self.klassifier[label][1]
        vals = map(int, values[:, 1])
        score = 0.0
        predictions = [1] * len(vals)
        for i, value in enumerate(vals):
            anomaly = 0
            if flag == 0:
                anomaly = (value - mean) / std > 3
            elif flag == 1:
                anomaly = (value - mean) / std < 3
            else:
                anomaly = abs(value - mean) / std > 3

            if anomaly == True:
                predictions[i] = -1
                score = score + 1
        return predictions, ((len(values) - score) / len(values))

# threeSigmaClassifier =    ThreeSigmaClassifier()

# threeSigmaClassifier.fit_transform('1', [1,4,7,9])
