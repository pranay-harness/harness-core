import argparse
import logging
import sys

from core.IsolationForestClassifier import IsolationForestClassifier
from core.KmeansAnomalyDetector import KmeansAnomalyDetector
from core.KmeansCluster import KmeansCluster
from core.TFIDFVectorizer import TFIDFVectorizer
from core.Tokenizer import Tokenizer
import numpy as np
from sources.SplunkDataset import SplunkDataset


class SplunkIntel(object):
    def __init__(self, cli_args):
        self._options = cli_args

    def detect_anomaly(self):
        logging.info('Running splunk anomaly detection')
        if self._options.file_source:
            self.run_from_file()
        else:
            self.run_from_splunk()

    def run_from_file(self):
        logging.info('Running using file source')
        splunkDataset = SplunkDataset()
        splunkDataset.load_from_file(self._options.file_source,
                                     self._options.control_window,
                                     self._options.test_window)
        logging.info(splunkDataset.get_all_events())

        vectorizer = TFIDFVectorizer(Tokenizer.default_tokenizer, 1, 1.0)
        tfidf_feature_matrix = vectorizer.fit_transform(splunkDataset.get_control_events_as_np())
        splunkDataset.set_feature_names(vectorizer.get_feature_names())
        splunkDataset.set_xy_matrix_control(vectorizer.get_cosine_dist_matrix(tfidf_feature_matrix))

        kmeans = KmeansCluster(tfidf_feature_matrix, self._options.sim_threshold)
        kmeans.cluster_cosine_threshold()

        splunkDataset.set_control_clusters(kmeans.get_clusters())
        splunkDataset.set_centroids(kmeans.get_centriods())

        tfidf_matrix_test = vectorizer.transform(np.array(splunkDataset.get_test_events_as_np()))
        newAnomDetector = KmeansAnomalyDetector()
        predictions, anomalies = np.array(
        newAnomDetector.detect_kmeans_anomaly_cosine_dist(tfidf_matrix_test, kmeans, self._options.sim_threshold))

        splunkDataset.set_test_clusters(predictions)
        splunkDataset.set_anomalies(anomalies)

        combined_vectorizer = TFIDFVectorizer(Tokenizer.default_tokenizer, 1, 1.0)
        combined_tfidf_matrix = combined_vectorizer.fit_transform(splunkDataset.get_all_events_as_np())

        combined_dist = combined_vectorizer.get_cosine_dist_matrix(combined_tfidf_matrix)

        splunkDataset.set_xy_matrix_all(combined_dist)

        control_groups = splunkDataset.get_control_values_pd().groupby('label')
        test_groups = splunkDataset.get_test_values_pd().groupby('label')

        classifier = IsolationForestClassifier()

        for name, group in control_groups:
            classifier.fit_transform(str(name), np.column_stack((group.x, group.y)))

        for name, group in test_groups:
            anomolous_values_predictions = classifier.predict(str(name), np.column_stack((group.x, group.y)))
            splunkDataset.set_anomalous_values(group.idx.tolist(), anomolous_values_predictions)

        return splunkDataset
        logging.info("done")


    def run_from_splunk(self):
        logging.info('Running using splunk')

    @staticmethod
    def parse(cli_args):
        parser = argparse.ArgumentParser()
        parser.add_argument("--file_source")
        parser.add_argument("--control_window", nargs='+', type=int)
        parser.add_argument("--test_window", nargs='+', type=int)
        parser.add_argument("--sim_threshold", type=float)
        parser.add_argument("--splunk_source")
        return parser.parse_args(cli_args)


def main(args):
    # simple log format
    format = "%(asctime)-15s %(levelname)s %(message)s"
    logging.basicConfig(level=logging.INFO, format=format)

    # create options
    options = SplunkIntel.parse(args[1:])

    logging.info(options)
    splunkIntel = SplunkIntel(options)
    splunkIntel.detect_anomaly()


if __name__ == "__main__":
    main(sys.argv)
