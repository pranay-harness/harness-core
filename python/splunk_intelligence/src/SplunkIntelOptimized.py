import argparse
import logging
import sys

import numpy as np

from core.KmeansAnomalyDetector import KmeansAnomalyDetector
from core.KmeansCluster import KmeansCluster
from core.TFIDFVectorizer import TFIDFVectorizer
from core.Tokenizer import Tokenizer
from sources.SplunkDatasetNew import SplunkDatasetNew
from core.FrequencyAnomalyDetector import FrequencyAnomalyDetector

format = "%(asctime)-15s %(levelname)s %(message)s"
logging.basicConfig(level=logging.INFO, format=format)
logger = logging.getLogger(__name__)


class SplunkIntelOptimized(object):
    def __init__(self, splunk_dataset_new, _options):
        self.splunkDatasetNew = splunk_dataset_new
        self._options = _options

    def run(self):
        logger.info('Running using file source')

        control_events = self.splunkDatasetNew.get_control_events()
        min_df = 1
        max_df = 1.0

        if len(control_events) > 500:
            min_df = 0.05
            max_df = 1.0

        logging.info("Start vectorization....")
        logger.info("setting min_df = " + str(min_df) + " and max_df = " + str(max_df))
        vectorizer = TFIDFVectorizer(Tokenizer.default_tokenizer, min_df, max_df)
        tfidf_feature_matrix = vectorizer.fit_transform(self.splunkDatasetNew.get_control_events_text_as_np())

        kmeans = KmeansCluster(tfidf_feature_matrix, self._options.sim_threshold)
        kmeans.cluster_cosine_threshold()

        tfidf_matrix_test = vectorizer.transform(np.array(self.splunkDatasetNew.get_test_events_text_as_np()))
        newAnomDetector = KmeansAnomalyDetector()

        predictions, anomalies = np.array(
            newAnomDetector.detect_kmeans_anomaly_cosine_dist(tfidf_matrix_test, kmeans, self._options.sim_threshold))

        combined_vectorizer = TFIDFVectorizer(Tokenizer.default_tokenizer, min_df, max_df)
        combined_tfidf_matrix = combined_vectorizer.fit_transform(self.splunkDatasetNew.get_all_events_text_as_np())

        combined_dist = combined_vectorizer.get_cosine_dist_matrix(combined_tfidf_matrix)

        self.splunkDatasetNew.create_clusters(combined_dist, kmeans.get_clusters(), kmeans.get_centriods(),
                                              vectorizer.get_feature_names(),
                                              predictions, anomalies)

        unknown_anomalies_text = self.splunkDatasetNew.get_unknown_anomalies_text()

        if len(unknown_anomalies_text) > 0:
            anom_vectorizer = TFIDFVectorizer(Tokenizer.default_tokenizer, min_df, max_df)
            tfidf_feature_matrix_anom = anom_vectorizer.fit_transform(np.array(unknown_anomalies_text))

            anom_kmeans = KmeansCluster(tfidf_feature_matrix_anom, self._options.sim_threshold)
            anom_kmeans.cluster_cosine_threshold()

            self.splunkDatasetNew.create_anom_clusters(anom_kmeans.get_clusters())

        logger.info("Detect Count Anomalies....")

        control_clusters = self.splunkDatasetNew.get_control_clusters()
        test_clusters = self.splunkDatasetNew.get_test_clusters()

        # classifier = IsolationForestClassifier()

        classifier = FrequencyAnomalyDetector()
        for idx, group in test_clusters.items():
            values = []
            for host, data in control_clusters[idx].items():
                values.extend(np.array([ freq.get('count') for freq in data.get('message_frequencies')]))

            # print(idx)
            # print(values)

            values_control = np.column_stack(([idx] * len(values), values))

            classifier.fit_transform(idx, values_control)

            for host, data in group.items():
                values_test = np.array([freq.get('count') for freq in data.get('message_frequencies')])
                # print(values_test)
                anomalous_counts, score = classifier.predict(idx, np.column_stack(([idx] * len(values_test), values_test)))
                # print(anomalous_counts)
                data.get('anomalous_counts').extend(anomalous_counts)
                if score < 0.5:
                    print(values)
                    print(values_test)
                    print(anomalous_counts)
                    data['unexpected_freq'] = True

        logger.info("done")
        return self.splunkDatasetNew

    @staticmethod
    def parse(cli_args):
        parser = argparse.ArgumentParser()
        parser.add_argument("--sim_threshold", type=float)
        return parser.parse_args(cli_args)


def parse(cli_args):
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", required=True)
    parser.add_argument("--application_id", required=True)
    parser.add_argument("--sim_threshold", type=float, required=True)
    parser.add_argument("--control_nodes", nargs='+', type=str, required=True)
    parser.add_argument("--test_nodes", nargs='+', type=str, required=True)
    parser.add_argument("--state_execution_id", type=str, required=True)
    parser.add_argument("--log_analysis_save_url", required=True)
    parser.add_argument("--log_analysis_get_url", required=True)
    parser.add_argument("--log_collection_minute", type=int, required=True)

    return parser.parse_args(cli_args)


def run_debug():
    control_start = 1
    test_start = 11

    prev_out_file = None
    while (control_start <= 2 or test_start < 12):
        splunkIntelargs = []
        splunkDataset = SplunkDatasetNew()

        print(control_start, control_start)
        print(test_start, test_start)
        splunkDataset.load_from_file('mzs.json',
                                     [control_start, control_start],
                                     [test_start, test_start], prev_out_file)

        splunkIntelargs.append('--sim_threshold')
        splunkIntelargs.append(str(float(0.8)))

        print(splunkIntelargs)

        splunkIntel = SplunkIntelOptimized(splunkDataset, SplunkIntelOptimized.parse(splunkIntelargs))
        splunkDataset = splunkIntel.run()

        file_object = open("result.json", "w")
        file_object.write(splunkDataset.get_output_as_json)
        file_object.close()

        prev_out_file = './result.json'

        control_start = control_start + 1
        test_start = test_start + 1


def main(args):

 #run_debug()

 #create options
 print(args)
 options = parse(args[1:])
 logging.info(options)

 splunkDataset = SplunkDatasetNew()

 splunkDataset.load_from_harness(options)

# Add the production flow to load data here

 splunkIntel = SplunkIntelOptimized(splunkDataset, options)
 splunkDataset = splunkIntel.run()

 logger.info(splunkDataset.save_to_harness(options.log_analysis_save_url, splunkDataset.get_output_as_json))


# result = {'args': args[1:], 'events': splunkDataset.get_all_events_as_json()}

# TODO post this to wings server once the api is available


if __name__ == "__main__":
    main(sys.argv)
