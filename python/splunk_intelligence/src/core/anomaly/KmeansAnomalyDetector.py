# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import numpy as np
from core.distance.JaccardDistance import jaccard_similarity, pairwise_jaccard_similarity
from sklearn.metrics.pairwise import cosine_similarity

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

        results = []
        for j, i in enumerate(predictions):
            mat = feature_matrix[j]
            sim = cosine_similarity(km.get_feature_matrix()[clusters == i], mat)
            cluster_cosine_sim = cosine_similarity(km.get_feature_matrix()[clusters == i])
            jaccard_sim = jaccard_similarity(km.get_feature_matrix()[clusters == i], mat)
            cluster_jaccard_sim = pairwise_jaccard_similarity(km.get_feature_matrix()[clusters == i])
            if len(np.where(sim < threshold)[0]) > 0:
                anomaly = -1
                score = np.min(sim)
                cluster_score = np.min(cluster_cosine_sim)
            elif len(np.where(jaccard_sim < threshold)[0]) > 0:
                anomaly = -1
                score = np.min(jaccard_sim)
                cluster_score = np.min(cluster_jaccard_sim)
            else:
                anomaly = 1
                score = np.min(jaccard_sim)
                cluster_score = np.min(cluster_jaccard_sim)

            # numpy types are not json serializable. convert to python datatypes
            results.append(
                dict(cluster_label=int(i), anomaly=anomaly, cluster_score=float(cluster_score), score=float(score)))

        return results
