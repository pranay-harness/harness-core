import argparse
import logging
import sys

import numpy as np
import time

from core.FrequencyAnomalyDetector import FrequencyAnomalyDetector
from core.KmeansAnomalyDetector import KmeansAnomalyDetector
from core.KmeansCluster import KmeansCluster
from core.TFIDFVectorizer import TFIDFVectorizer
from core.Tokenizer import Tokenizer
from sources.SplunkDatasetNew import SplunkDatasetNew
from core.JaccardDistance import jaccard_difference, jaccard_text_similarity
from multiprocessing import Process

format = "%(asctime)-15s %(levelname)s %(message)s"
logging.basicConfig(level=logging.INFO, format=format)
logger = logging.getLogger(__name__)


class SplunkIntelOptimized(object):
    def __init__(self, splunk_dataset_new, _options):
        self.splunkDatasetNew = splunk_dataset_new
        self._options = _options

    def create_feature_vector(self, texts, min_df=1, max_df=1.0):
        processed = False
        while not processed:
            try:
                combined_vectorizer = TFIDFVectorizer(Tokenizer.default_tokenizer, min_df, max_df)
                combined_tfidf_matrix = combined_vectorizer.fit_transform(texts)
                logging.info("Finish create combined dist")
                processed = True
            except ValueError:
                if max_df == 1.0:
                    raise
                max_df = 1.0
        if combined_tfidf_matrix is None or combined_tfidf_matrix is None:
            logger.error("Unable to vectorize texts for min_df = " + str(min_df) + " max_df = " + str(max_df))
            sys.exit(-1)
        return combined_vectorizer, combined_tfidf_matrix

    # TODO run in parallel
    def set_xy(self):
        logging.info("Create combined dist")
        min_df = 1
        max_df = 0.99 if len(self.splunkDatasetNew.get_events_for_xy()) > 1 else 1.0
        combined_vectorizer, combined_tfidf_matrix = self.create_feature_vector(
            self.splunkDatasetNew.get_events_for_xy(), min_df, max_df)

        logging.info("Finish create combined dist")

        dist_matrix = combined_vectorizer.get_cosine_dist_matrix(combined_tfidf_matrix)
        self.splunkDatasetNew.set_xy(dist_matrix)

    def create_anom_clusters(self):

        logging.info("Create anomalous clusters")

        unknown_anomalies_text = self.splunkDatasetNew.get_unknown_anomalies_text()

        if len(unknown_anomalies_text) > 0:
            min_df = 1
            max_df = 0.99 if len(unknown_anomalies_text) > 1 else 1.0

            anom_vectorizer, tfidf_feature_matrix_anom = self.create_feature_vector(np.array(unknown_anomalies_text),
                                                                                    min_df, max_df)

            anom_kmeans = KmeansCluster(tfidf_feature_matrix_anom, self._options.sim_threshold)
            anom_kmeans.cluster_cosine_threshold()

            self.splunkDatasetNew.create_anom_clusters(anom_kmeans.get_clusters())

            control_clusters = self.splunkDatasetNew.get_control_clusters()
            anom_clusters = self.splunkDatasetNew.get_anom_clusters()
            for key, anomalies in anom_clusters.items():
                for host, anomaly in anomalies.items():
                    score = jaccard_text_similarity([control_clusters[anomaly['cluster_label']].values()[0]['text']],
                                                    anomaly['text'])
                    if 0.9 > score[0] > 0.5:
                        anomaly['diff_tags'] = []
                        anomaly['diff_tags'].extend(
                            jaccard_difference(control_clusters[anomaly['cluster_label']].values()[0]['text'],
                                               anomaly['text']))

        logging.info("Finish create anomolous clusters")

    def cluster_input(self):
        # TODO Can min_df be set higher or max_df set lower
        min_df = 1
        max_df = 0.99 if len(self.splunkDatasetNew.get_control_events_text_as_np()) > 1 else 1.0
        logger.info("setting min_df = " + str(min_df) + " and max_df = " + str(max_df))
        logging.info("Start vectorization....")
        vectorizer, tfidf_feature_matrix = self.create_feature_vector(
            self.splunkDatasetNew.get_control_events_text_as_np(), min_df, max_df)

        kmeans = KmeansCluster(tfidf_feature_matrix, self._options.sim_threshold)
        kmeans.cluster_cosine_threshold()

        logging.info("Finish kemans....")

        return vectorizer, kmeans

    def detect_count_anomalies(self):

        logger.info("Detect Count Anomalies....")

        control_clusters = self.splunkDatasetNew.get_control_clusters()
        test_clusters = self.splunkDatasetNew.get_test_clusters()

        classifier = FrequencyAnomalyDetector()

        for idx, group in test_clusters.items():
            values = []
            for host, data in control_clusters[idx].items():
                values.extend(np.array([freq.get('count') for freq in data.get('message_frequencies')]))

            # print(idx)
            # print(values)

            values_control = np.column_stack(([idx] * len(values), values))

            classifier.fit_transform(idx, values_control)

            for host, data in group.items():
                values_test = np.array([freq.get('count') for freq in data.get('message_frequencies')])
                # print(values_test)
                anomalous_counts, score = classifier.predict(idx,
                                                             np.column_stack(([idx] * len(values_test), values_test)))
                # print(anomalous_counts)
                data.get('anomalous_counts').extend(anomalous_counts)
                if score < 0.5:
                    print('values=', values)
                    print('values_test=', values_test)
                    print(anomalous_counts)
                    data['unexpected_freq'] = True

        logger.info("Finish detect count Anomalies....")

    def detect_unknown_events(self, vectorizer, kmeans):
        logging.info("Detect unknown events")
        predictions = []
        anomalies = []
        if bool(self.splunkDatasetNew.test_events):
            tfidf_matrix_test = vectorizer.transform(np.array(self.splunkDatasetNew.get_test_events_text_as_np()))
            newAnomDetector = KmeansAnomalyDetector()

            predictions, anomalies = np.array(
                newAnomDetector.detect_kmeans_anomaly_cosine_dist(tfidf_matrix_test,
                                                                  kmeans, self._options.sim_threshold))

        logging.info("Finish detect unknown events")
        return predictions, anomalies

    def run(self):

        start_time = time.time()

        logger.info("Running analysis")

        if not bool(self.splunkDatasetNew.control_events):
            logger.warn("No control events. Nothing to do")
            return self.splunkDatasetNew

        vectorizer, kmeans = self.cluster_input()

        predictions, anomalies = self.detect_unknown_events(vectorizer, kmeans)

        self.splunkDatasetNew.create_clusters(kmeans.get_clusters(), kmeans.get_centriods(),
                                              vectorizer.get_feature_names(),
                                              predictions, anomalies)

        self.create_anom_clusters()

        self.detect_count_anomalies()

        self.set_xy()

        logger.info("done. time taken " + str(time.time() - start_time) + " seconds")
        return self.splunkDatasetNew

    @staticmethod
    def parse(cli_args):
        parser = argparse.ArgumentParser()
        parser.add_argument("--sim_threshold", type=float)
        return parser.parse_args(cli_args)


def parse(cli_args):
    parser = argparse.ArgumentParser()
    parser.add_argument("--control_input_url", required=True)
    parser.add_argument("--test_input_url", required=False)
    parser.add_argument("--auth_token", required=True)
    parser.add_argument("--application_id", required=True)
    parser.add_argument("--workflow_id", required=True)
    parser.add_argument("--service_id", required=True)
    parser.add_argument("--sim_threshold", type=float, required=True)
    parser.add_argument("--control_nodes", nargs='+', type=str, required=True)
    parser.add_argument("--test_nodes", nargs='+', type=str, required=False)
    parser.add_argument("--state_execution_id", type=str, required=True)
    parser.add_argument("--log_analysis_save_url", required=True)
    parser.add_argument("--log_analysis_get_url", required=True)
    parser.add_argument("--query", required=True)
    parser.add_argument("--log_collection_minute", type=int, required=True)

    return parser.parse_args(cli_args)


def run_debug_live_traffic(options):
    control_start = 0
    test_start = 0

    prev_out_file = None
    while control_start <= 13 or test_start < 13:

        splunk_dataset = SplunkDatasetNew()

        print(control_start, control_start)
        print(test_start, test_start)
        splunk_dataset.load_prod_file(
            '/Users/sriram_parthasarathy/wings/python/splunk_intelligence/data_prod/prodOut1.json',
            [control_start, control_start],
            [test_start, test_start], ['ip-172-31-28-126'], ['ip-172-31-19-157'], prev_out_file)

        print(options)

        if splunk_dataset.new_data:
            splunk_intel = SplunkIntelOptimized(splunk_dataset, options)
            splunk_dataset = splunk_intel.run()

            file_object = open("result.json", "w")
            file_object.write(splunk_dataset.get_output_as_json(options))
            file_object.close()
            prev_out_file = './result.json'

        control_start = control_start + 1
        test_start = test_start + 1


def run_debug_prev_run(options):
    control_start = 1
    test_start = 1

    prev_out_file = None
    while control_start <= 1 or test_start < 1:

        splunk_dataset = SplunkDatasetNew()

        print(control_start, control_start)
        print(test_start, test_start)
        splunk_dataset.load_prod_file_prev_run(
            '/Users/sriram_parthasarathy/wings/python/splunk_intelligence/data_prod/prev_run/control.json',
            [control_start, control_start],
            '/Users/sriram_parthasarathy/wings/python/splunk_intelligence/data_prod/prev_run/test.json',
            [test_start, test_start], prev_out_file)

        print(options)

        if splunk_dataset.new_data:
            splunk_intel = SplunkIntelOptimized(splunk_dataset, options)
            splunk_dataset = splunk_intel.run()

            file_object = open("result.json", "w")
            file_object.write(splunk_dataset.get_output_as_json(options))
            file_object.close()
            prev_out_file = './result.json'

        control_start = control_start + 1
        test_start = test_start + 1


def main(args):
    logger.info(args)
    options = parse(args[1:])
    logger.info(options)

    #run_debug_prev_run(options)

    splunk_dataset = SplunkDatasetNew()

    splunk_dataset.load_from_harness(options)

    splunk_intel = SplunkIntelOptimized(splunk_dataset, options)
    splunk_dataset = splunk_intel.run()

    logger.info(splunk_dataset.save_to_harness(options.log_analysis_save_url, options.auth_token,
                                               splunk_dataset.get_output_as_json(options)))


if __name__ == "__main__":
    main(sys.argv)
