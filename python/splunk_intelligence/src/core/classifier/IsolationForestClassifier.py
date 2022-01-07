# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import numpy as np
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import StandardScaler

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
