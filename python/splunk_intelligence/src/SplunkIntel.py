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
from core.ThreeSigmaClassifier import ThreeSigmaClassifier

format = "%(asctime)-15s %(levelname)s %(message)s"
logging.basicConfig(level=logging.INFO, format=format)
logger = logging.getLogger(__name__)

class SplunkIntel(object):
    def __init__(self, splunk_dataset, _options):
        self.splunkDataset = splunk_dataset
        self._options = _options

    def run(self):
        logger.info('Running using file source')

        total_events = len(self.splunkDataset.get_all_events())

        logger.info("Total events = " + str(total_events))

        min_df = 1.0
        max_df = 1.0

        if total_events > 100:
            logger.info("setting min_df = 0.05 and max_df = 0.9")
            min_df = 0.05
            max_df = 1.0

        logging.info("Start vectorization....")
        vectorizer = TFIDFVectorizer(Tokenizer.default_tokenizer, min_df, max_df)
        tfidf_feature_matrix = vectorizer.fit_transform(self.splunkDataset.get_control_events_text_as_np())
        self.splunkDataset.set_feature_names(vectorizer.get_feature_names())
        self.splunkDataset.set_xy_matrix_control(vectorizer.get_cosine_dist_matrix(tfidf_feature_matrix))

        logger.info("Start clustering....")

        kmeans = KmeansCluster(tfidf_feature_matrix, self._options.sim_threshold)
        kmeans.cluster_cosine_threshold()

        self.splunkDataset.set_control_clusters(kmeans.get_clusters())
        self.splunkDataset.set_centroids(kmeans.get_centriods())

        logger.info("Detect unknown events....")

        tfidf_matrix_test = vectorizer.transform(np.array(self.splunkDataset.get_test_events_text_as_np()))
        newAnomDetector = KmeansAnomalyDetector()
        predictions, anomalies = np.array(
            newAnomDetector.detect_kmeans_anomaly_cosine_dist(tfidf_matrix_test, kmeans, self._options.sim_threshold))

        self.splunkDataset.set_test_clusters(predictions)
        self.splunkDataset.set_anomalies(anomalies)

        logger.info("Combined Vectorizer....")

        combined_vectorizer = TFIDFVectorizer(Tokenizer.default_tokenizer, min_df, max_df)
        combined_tfidf_matrix = combined_vectorizer.fit_transform(self.splunkDataset.get_all_events_text_as_np())

        combined_dist = combined_vectorizer.get_cosine_dist_matrix(combined_tfidf_matrix)

        self.splunkDataset.set_xy_matrix_all(combined_dist)

        logger.info("Detect Count Anomalies....")

        control_groups = self.splunkDataset.get_control_values_pd().groupby('label')
        test_groups = self.splunkDataset.get_test_values_pd().groupby('label')

        # classifier = IsolationForestClassifier()

        classifier = ThreeSigmaClassifier()

        for name, group in control_groups:
            classifier.fit_transform(str(name), np.column_stack((group.x, group.y)))

        for name, group in test_groups:
            anomolous_values_predictions = classifier.predict(str(name), np.column_stack((group.x, group.y)))
            self.splunkDataset.set_anomalous_values(group.idx.tolist(), anomolous_values_predictions)

        logger.info("done")
        return self.splunkDataset

    def run_from_splunk(self):
        logging.info('Running using splunk')

    @staticmethod
    def parse(cli_args):
        parser = argparse.ArgumentParser()
        parser.add_argument("--sim_threshold", type=float)
        return parser.parse_args(cli_args)


def parse(cli_args):
    parser = argparse.ArgumentParser()
    parser.add_argument("--control_input_url", required=True)
    parser.add_argument("--test_input_url", required=True)
    parser.add_argument("--application_id", required=True)
    parser.add_argument("--workflow_id", required=True)
    parser.add_argument("--control_window", nargs='+', type=int, required=True)
    parser.add_argument("--test_window", nargs='+', type=int, required=True)
    parser.add_argument("--sim_threshold", type=float, required=True)
    parser.add_argument("--control_nodes", nargs='+', type=str, required=True)
    parser.add_argument("--test_nodes", nargs='+', type=str, required=True)
    return parser.parse_args(cli_args)


def main(args):

    # create options
    options = parse(args[1:])
    logging.info(options)

    splunkDataset = SplunkDataset()

    splunkDataset.load_from_harness(options)

    # Add the production flow to load data here

    splunkIntel = SplunkIntel(splunkDataset, options)
    splunkIntel.run()

    result = {'args': args[1:], 'events': splunkDataset.get_all_events_as_json()}
    print(json.dumps(result))

    #TODO post this to wings server once the api is available


if __name__ == "__main__":
    main(sys.argv)
