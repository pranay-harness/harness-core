import numpy as np


class ZeroDeviationClassifier(object):
    klassifier = {}

    def fit_transform(self, label, values, threshold):
        vals = map(int, values[:, 1])
        np_values = np.array(vals)
        self.klassifier[label] = [np.mean(np_values, axis=0), threshold]

    def predict(self, label, values):
        mean = self.klassifier[label][0]
        threshold = self.klassifier[label][1]
        vals = map(int, values[:, 1])
        score = 0.0
        predictions = [1] * len(vals)
        for i, value in enumerate(vals):
            if abs(value - mean) / mean > threshold:
                predictions[i] = -1
                score = score + 1
        return predictions, ((len(values) - score) / len(values))
