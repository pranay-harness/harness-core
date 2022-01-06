# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import numpy as np
from core.distance.JaccardDistance import pairwise_jaccard_similarity
from core.util.lelogging import get_log
from sklearn.cluster import KMeans
from sklearn.metrics.pairwise import cosine_similarity

"""
Wrapper class for Kmeans clustering
"""

logger = get_log(__name__)

class KmeansCluster(object):

    km = None
    num_clusters = None

    def __init__(self, feature_matrix, threshold):
        """

        :param feature_matrix: the feature matrix. The row is the number
                               of samples to cluster, and the columns form
                               the feature vector for each sample.
        :param threshold: the homogeneity threshold. Kmeans will be terminted
                        if all min(pairwise homogenity score) > threshold for
                        all clusters
        """
        #TODO this can potentially take a homogeneity function to
        #TODO calculate the homogeneity score for any pair
        self.feature_matrix = feature_matrix
        self.theshold = threshold

    def cluster_cosine_threshold(self):
        """
        Run Kmeans using cosine similarity as the homogeneity score.
        Does a binary search from K is 1 to len(no of samples) and finds
        the minimum K for which min(pairwise homogenity score) > threshold for
        all clusters. Saves the kmeans object to km

        :return: Nothing
        """
        lower = 1
        upper = self.feature_matrix.shape[0]
        mid = min(100, (int)(lower + (upper - lower) / 2))
        while lower <= upper:
            logger.info("Running kemans with k = " + str(mid))
            curr_km = KMeans(n_clusters=mid, n_jobs=-1)
            curr_km.fit(self.feature_matrix)
            clusters = np.array(curr_km.labels_.tolist())
            found = True
            for j in set(clusters):
                sim = cosine_similarity(self.feature_matrix[clusters == j])
                if len(np.where(sim < self.theshold)[0]) > 0:
                    found = False
                    lower = mid + 1
                    break
                elif len(np.where(pairwise_jaccard_similarity(self.feature_matrix[clusters == j])
                                          < self.theshold)[0]) > 0:
                    found = False
                    lower = mid + 1
                    break
            if found:
                logger.info("found k = " + str(mid))
                self.num_clusters = mid
                self.km = curr_km
                upper = mid - 1

            mid = int(lower + (upper - lower) / 2)


    def get_clusters(self):
        """

        :return: the cluster assignments
        """
        return self.km.labels_.tolist()

    def get_num_clusters(self):
        """

        :return: the total number of clusters
        """
        return self.num_clusters

    def get_centriods(self):
        """

        :return: the centroids for the clusters
        """
        return self.km.cluster_centers_.argsort()[:, ::-1]

    def get_feature_matrix(self):
        """

        :return: the inout feature matrix
        """
        return self.feature_matrix

    def predict(self, feature_matrix):
        """

        :param feature_matrix: the feature matrix for the test samples
        :return: the cluster assignments for the test samples
        """
        return self.km.predict(feature_matrix)
