import numpy as np
from sklearn.metrics.pairwise import cosine_similarity

"""
Detect anomalies based on kmeans cluster
"""
class KmeansAnomalyDetector(object):


    def detect_kmeans_anomaly_cosine_dist(self, feature_matrix, km, threshold):

        """

        :param feature_matrix: the test feature matrix
        :param km: the kmeans cluster
        :param threshold: the cosine threshold
        :return: the cluster assignments, or -1 if anomaly
        """

        clusters = np.array(km.get_clusters())

        predictions = km.predict(feature_matrix)

        result = []

        for j, i in enumerate(predictions):
            mat = feature_matrix[j]
            sim = cosine_similarity(km.get_feature_matrix()[clusters == i], mat)
            if len(np.where(sim < threshold)[0]) > 0:
                result.append(-1)
            else:
                result.append(i)

        return result