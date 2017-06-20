import argparse
import logging
import sys

import numpy as np

from core.IsolationForestClassifier import IsolationForestClassifier
from core.KmeansAnomalyDetector import KmeansAnomalyDetector
from core.KmeansCluster import KmeansCluster
from core.TFIDFVectorizer import TFIDFVectorizer
from core.Tokenizer import Tokenizer
from sources.SplunkDataset import SplunkDataset


class SplunkIntel(object):
    def __init__(self, splunk_dataset, _options):
        self.splunkDataset = splunk_dataset
        self._options = _options

    def detect_anomaly(self):
        logging.info('Running splunk anomaly detection')

    def run(self):
        logging.info('Running using file source')

        vectorizer = TFIDFVectorizer(Tokenizer.default_tokenizer, 1, 1.0)
        tfidf_feature_matrix = vectorizer.fit_transform(self.splunkDataset.get_control_events_as_np())
        self.splunkDataset.set_feature_names(vectorizer.get_feature_names())
        self.splunkDataset.set_xy_matrix_control(vectorizer.get_cosine_dist_matrix(tfidf_feature_matrix))

        kmeans = KmeansCluster(tfidf_feature_matrix, self._options.sim_threshold)
        kmeans.cluster_cosine_threshold()

        self.splunkDataset.set_control_clusters(kmeans.get_clusters())
        self.splunkDataset.set_centroids(kmeans.get_centriods())

        tfidf_matrix_test = vectorizer.transform(np.array(self.splunkDataset.get_test_events_as_np()))
        newAnomDetector = KmeansAnomalyDetector()
        predictions, anomalies = np.array(
            newAnomDetector.detect_kmeans_anomaly_cosine_dist(tfidf_matrix_test, kmeans, self._options.sim_threshold))

        self.splunkDataset.set_test_clusters(predictions)
        self.splunkDataset.set_anomalies(anomalies)

        combined_vectorizer = TFIDFVectorizer(Tokenizer.default_tokenizer, 1, 1.0)
        combined_tfidf_matrix = combined_vectorizer.fit_transform(self.splunkDataset.get_all_events_as_np())

        combined_dist = combined_vectorizer.get_cosine_dist_matrix(combined_tfidf_matrix)

        self.splunkDataset.set_xy_matrix_all(combined_dist)

        control_groups = self.splunkDataset.get_control_values_pd().groupby('label')
        test_groups = self.splunkDataset.get_test_values_pd().groupby('label')

        classifier = IsolationForestClassifier()

        for name, group in control_groups:
            classifier.fit_transform(str(name), np.column_stack((group.x, group.y)))

        for name, group in test_groups:
            anomolous_values_predictions = classifier.predict(str(name), np.column_stack((group.x, group.y)))
            self.splunkDataset.set_anomalous_values(group.idx.tolist(), anomolous_values_predictions)

        return self.splunkDataset
        logging.info("done")

    def run_from_splunk(self):
        logging.info('Running using splunk')

    @staticmethod
    def parse(cli_args):
        parser = argparse.ArgumentParser()
        parser.add_argument("--sim_threshold", type=float)
        return parser.parse_args(cli_args)


def main(args):
    # simple log format
    format = "%(asctime)-15s %(levelname)s %(message)s"
    logging.basicConfig(level=logging.INFO, format=format)

    # create options
    options = SplunkIntel.parse(args[1:])
    logging.info(options)

    splunkDataset = SplunkDataset()

    # Add the production flow to load data here

    splunkIntel = SplunkIntel(splunkDataset, options)
    splunkIntel.run()


if __name__ == "__main__":
    main(sys.argv)
