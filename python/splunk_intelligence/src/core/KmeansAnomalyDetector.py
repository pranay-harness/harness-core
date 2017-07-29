import numpy as np
from sklearn.metrics.pairwise import cosine_similarity
from JaccardDistance import jaccard_similarity
"""
Detect anomalies based on kmeans clusters
"""
class KmeansAnomalyDetector(object):


    def detect_kmeans_anomaly_cosine_dist(self, feature_matrix, km, threshold):

        """

        :param feature_matrix: the test feature matrix
        :param km: the kmeans cluster
        :param threshold: the cosine threshold
        :return: the cluster assignments,and anomalies ( 1 if normal, -1 if anomaly)
        """

        clusters = np.array(km.get_clusters())

        predictions = km.predict(feature_matrix)

        anomalies = []
        for j, i in enumerate(predictions):
            mat = feature_matrix[j]
            sim = cosine_similarity(km.get_feature_matrix()[clusters == i], mat)
            jaccard_sim = jaccard_similarity(km.get_feature_matrix()[clusters == i], mat)
            if len(np.where(sim < threshold)[0]) > 0:
                anomalies.append(-1)
            elif len(np.where( jaccard_sim < threshold)[0]) > 0:
                anomalies.append(-1)
            else:
                anomalies.append(1)

        return predictions,anomalies