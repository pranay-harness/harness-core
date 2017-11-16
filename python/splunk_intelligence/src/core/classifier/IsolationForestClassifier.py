from sklearn.preprocessing import StandardScaler
from sklearn.ensemble import IsolationForest

import numpy as np

"""
Wrapper class to encapsulate random forest classifiers to detect anomalies
"""
class IsolationForestClassifier(object):

    klassifier = {}
    scaler = {}


    def fit_transform(self, label, values):
        """

        :param label: the key to identify the classifier
        :param values: the initial source values
        :return: Nothing
        """
        rng = np.random.RandomState(42)
        self.klassifier[label] = IsolationForest(contamination=0.001, random_state=rng)
        self.scaler[label] = StandardScaler()
        pq = self.scaler[label].fit_transform(values)
        self.klassifier[label].fit(pq)


    def predict(self, label, values):
        """

        :param label: the key to identify the classifier
        :param values: the values for prediction
        :return: a list of containing -1 or 1. -1 denotes anomaly
        """
        pq = self.scaler[label].transform(values)
        score = (len(values) - len(np.where(pq < 1))) / len(values)
        return self.klassifier[label].predict(pq), score
