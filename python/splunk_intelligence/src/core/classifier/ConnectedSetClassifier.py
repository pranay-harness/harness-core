# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import numpy as np
import scipy.spatial.distance as ssd
from core.anomaly.FrequencyAnomalyDetector import FrequencyAnomalyDetector
from sklearn.cluster import DBSCAN
from sklearn.neighbors import KNeighborsClassifier
from sklearn.preprocessing import StandardScaler


class ConnectedSetClassifier(object):
    def __init__(self, klassifier):
        self.klassifier = klassifier
        self.labels = {}

    def fit_transform(self, label, values, threshold):
        results = []
        grouped_values = [values]
        for grouped_val in grouped_values:
            k = grouped_val.shape[0]
            while k > 1:
                scaled_values = StandardScaler().fit_transform(grouped_val)
                knn = KNeighborsClassifier(k)
                knn.fit(scaled_values, list(range(scaled_values.shape[0])))
                dist, neighbors = knn.kneighbors(scaled_values, return_distance=True)
                epsilon = np.median(dist[:, k - 1], axis=0) + 0.0000001
                # if np.median(ssd.pdist(scaled_values)) / epsilon < threshold:
                #    results.append(grouped_val)
                #    break
                dbscan_classifier = DBSCAN(eps=epsilon, min_samples=1)
                predictions = dbscan_classifier.fit_predict(scaled_values)
                if len(set(predictions)) == 1:
                    k = k - 1
                    continue
                for x in set(predictions):
                    if len(scaled_values[predictions == x]) == 1:
                        results.append(grouped_val[predictions == x])
                    elif np.median(ssd.pdist(scaled_values[predictions == x])) / epsilon > threshold:
                        # print(np.median(ssd.pdist(scaled_values[predictions == x])))
                        # print('------', grouped_val[predictions == x])
                        grouped_values.append(grouped_val[predictions == x])
                    else:
                        results.append(grouped_val[predictions == x])
                break
            if k == 1:
                results.append(grouped_val)

        self.labels[label] = []
        for i, result in enumerate(results):
            l = str(label) + '-' + str(i)
            self.klassifier.fit_transform(l, result)
            self.labels[label].append(l)

    def predict(self, label, values):
        predictions = np.array([-1] * len(values))
        for l in self.labels[label]:
            for i, p in enumerate(self.klassifier.predict(l, values)[0]):
                if p == 1:
                    predictions[i] = 1

        print(predictions, len(predictions[predictions == -1]), (len(predictions[predictions == -1]) / len(values)))
        return predictions, ((len(values) - len(predictions[predictions == -1])) / len(values))


x = [100, 31, 64, 64, 65, 64, 52, 50, 48, 52, 51, 52, 50, 52, 51, 52, 31, 64, 64, 65, 64, 52, 51, 49, 51, 51, 52, 50,
     52, 50, 51, 31, 64, 64, 65, 64, 52, 51, 49, 51, 52, 51, 50, 52, 50, 51, 30, 64, 63, 65, 64, 52, 50, 48, 52, 51, 52,
     50, 52, 51, 52]

y = [96, 3, 135, 126, 138, 135, 135, 135, 138, 135, 135]

x = [[1, i] for i in x]
y = [[1, i] for i in y]

# klassifier = IsolationForestClassifier()
# klassifier.fit_transform(1,x)
# print(klassifier.predict(1,y))

# [u'292', u'1561', u'1686', u'2385', u'257', u'3225', u'352', u'4210',
# u'5396', u'8086', u'8368', u'931']
# ['1' , '10000', '100000000']


# vals = []
# for x in [u'292', u'1561', u'1686', u'2385', u'257', u'3225', u'352', u'4210',
#          u'5396', u'8086', u'8368', u'931']:
#    vals.append(['1', x])

# x = [[1,100], [1,10000]]
# y = [[1,400]]
clssifier = ConnectedSetClassifier(FrequencyAnomalyDetector())
clssifier.fit_transform(1, np.array(x), 0.2)
print(clssifier.predict(1, np.array(y)))
