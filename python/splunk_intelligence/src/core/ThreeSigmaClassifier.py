import numpy as np


class ThreeSigmaClassifier(object):
    klassifier = {}

    def fit_transform(self, label, values):
        vals = map(int, values[:, 1])
        np_values = np.array(vals)
        self.klassifier[label] = [np.mean(np_values, axis=0), np.std(np_values, axis=0)]

    def predict(self, label, values):
        mean = self.klassifier[label][0]
        std = self.klassifier[label][1]
        vals = map(int, values[:, 1])
        predictions = [1] * len(vals)
        for i, value in enumerate(vals):
            if abs(value - mean) > 3 * std:
                predictions[i] = -1
        return predictions

# threeSigmaClassifier =    ThreeSigmaClassifier()

# threeSigmaClassifier.fit_transform('1', [1,4,7,9])
