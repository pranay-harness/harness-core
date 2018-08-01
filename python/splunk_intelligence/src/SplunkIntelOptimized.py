import argparse
import json
import sys
import time
import numpy as np
import os
from sklearn.metrics.pairwise import euclidean_distances

from core.anomaly.FrequencyAnomalyDetector import FrequencyAnomalyDetector
from core.anomaly.KmeansAnomalyDetector import KmeansAnomalyDetector
from core.cluster.KmeansCluster import KmeansCluster
from core.distance.JaccardDistance import jaccard_difference, jaccard_text_similarity
from core.feature.TFIDFVectorizer import TFIDFVectorizer
from core.feature.Tokenizer import Tokenizer
from sources.LogCorpus import LogCorpus
from core.util.lelogging import get_log

logger = get_log(__name__)

if os.environ.get('cluster_limit'):
    cluster_limit = os.environ.get('cluster_limit')
else:
    cluster_limit = 500


class SplunkIntelOptimized(object):
    def __init__(self, corpus, _options):
        self.corpus = corpus
        self._options = _options

    def create_feature_vector(self, texts, min_df=1, max_df=1.0):
        processed = False
        while not processed:
            try:
                combined_vectorizer = TFIDFVectorizer(Tokenizer.default_tokenizer, min_df, max_df)
                combined_tfidf_matrix = combined_vectorizer.fit_transform(texts)
                if combined_tfidf_matrix[np.diff(combined_tfidf_matrix.indptr) == 0].shape[0] > 0:
                    raise ValueError('Unable to featurize text with max_df = ' + str(max_df))
                logger.info("Finish create combined dist")
                processed = True
            except ValueError:
                if max_df == 1.0:
                    raise
                max_df = 1.0
        if combined_tfidf_matrix is None or combined_vectorizer is None:
            raise Exception("Unable to vectorize texts for min_df = " + str(min_df) + " max_df = " + str(max_df))
        return combined_vectorizer, combined_tfidf_matrix

    # TODO run in parallel
    def set_xy(self):
        logger.info("Create combined dist")
        min_df = 1
        max_df = 0.99 if len(self.corpus.get_events_for_xy()) > 1 else 1.0
        combined_vectorizer, combined_tfidf_matrix = self.create_feature_vector(
            self.corpus.get_events_for_xy(), min_df, max_df)

        logger.info("Finish create combined dist")

        dist_matrix = combined_vectorizer.get_cosine_dist_matrix(combined_tfidf_matrix)
        self.corpus.set_xy(dist_matrix)

    def create_anom_clusters(self):

        logger.info("Create anomalous clusters")

        unknown_anomalies_text = self.corpus.get_unknown_anomalies_text()

        if len(unknown_anomalies_text) > 0:
            min_df = 1
            max_df = 0.99 if len(unknown_anomalies_text) > 1 else 1.0

            anom_vectorizer, tfidf_feature_matrix_anom = self.create_feature_vector(np.array(unknown_anomalies_text),
                                                                                    min_df, max_df)

            anom_kmeans = KmeansCluster(tfidf_feature_matrix_anom, self._options.sim_threshold)
            anom_kmeans.cluster_cosine_threshold()

            self.corpus.create_anom_clusters(anom_kmeans.get_clusters())

            control_clusters = self.corpus.get_control_clusters()
            anom_clusters = self.corpus.get_anom_clusters()
            feedback_clusters = self.corpus.get_feedback_clusters()
            ignore_clusters = self.corpus.get_ignore_clusters()
            for key, anomalies in anom_clusters.items():
                for host, anomaly in anomalies.items():
                    if anomaly['control_label'] in control_clusters:
                        base_text = control_clusters[anomaly['control_label']].values()[0]['text']
                    elif anomaly['control_label'] in ignore_clusters:
                        base_text = ignore_clusters[anomaly['control_label']].values()[0]['text']
                    elif anomaly['control_label'] in feedback_clusters:
                        base_text = feedback_clusters[anomaly['control_label']]['text']



                    score = jaccard_text_similarity([base_text],
                                                    anomaly['text'])
                    anomaly['alert_score'] = score[0]
                    if 0.9 > score[0] > 0.5:
                        anomaly['diff_tags'] = []
                        anomaly['diff_tags'].extend(
                            jaccard_difference(base_text,
                                               anomaly['text']))

        logger.info("Finish create anomolous clusters")

    def cluster_input(self):
        # TODO Can min_df be set higher or max_df set lower
        min_df = 1
        max_df = 0.99 if len(self.corpus.get_control_events_text_as_np()) > 1 else 1.0
        logger.info("setting min_df = " + str(min_df) + " and max_df = " + str(max_df))
        logger.info("Start vectorization....")
        vectorizer, tfidf_feature_matrix = self.create_feature_vector(
            self.corpus.get_control_events_text_as_np(), min_df, max_df)

        kmeans = KmeansCluster(tfidf_feature_matrix, self._options.sim_threshold)
        kmeans.cluster_cosine_threshold()

        logger.info("Finish kemans....")

        return vectorizer, kmeans

    def detect_count_anomalies(self):

        logger.info("Detect Count Anomalies....")

        control_clusters = self.corpus.get_control_clusters()
        test_clusters = self.corpus.get_test_clusters()

        classifier = FrequencyAnomalyDetector()

        for idx, group in test_clusters.items():
            values = []
            for host, data in control_clusters[idx].items():
                values.extend(np.array([freq.get('count') for freq in data.get('message_frequencies')]))

            # print(idx)
            # print(values)

            values_control = np.column_stack(([idx] * len(values), values))

            classifier.fit_transform(idx, values_control)
            max_score = 0.0
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
                    data['freq_score'] = round(1 - score, 2)
                    if data['freq_score'] > max_score:
                        if idx not in self.corpus.cluster_scores['test']:
                            self.corpus.cluster_scores['test'][idx] = {}
                        self.corpus.cluster_scores['test'][idx]['unexpected_freq'] = True
                        self.corpus.cluster_scores['test'][idx]['freq_score'] = data['freq_score']
                        max_score = data['freq_score']

        logger.info("Finish detect count Anomalies....")

    def global_score(self):
        control_scores = []
        test_scores = []

        for anom_cluster in self.corpus.cluster_scores['unknown'].values():
            control_scores.append(anom_cluster['control_score'])
            test_scores.append(1 - anom_cluster['test_score'])

        for test_cluster in self.corpus.cluster_scores['test'].values():
            if test_cluster['unexpected_freq']:
                control_scores.append(1.0)
                test_scores.append(1 - test_cluster['freq_score'])

        if len(control_scores) > 0:
            self.corpus.score = round(
                euclidean_distances([control_scores], [test_scores]).flatten()[0], 2)




    def detect_unknown_events(self, vectorizer, kmeans):
        logger.info("Detect unknown events")
        predictions = []
        if bool(self.corpus.test_events):
            tfidf_matrix_test = vectorizer.transform(np.array(self.corpus.get_test_events_text_as_np()))
            new_anom_detector = KmeansAnomalyDetector()
            predictions = np.array(
                new_anom_detector.detect_kmeans_anomaly_cosine_dist(tfidf_matrix_test,
                                                                  kmeans, self._options.sim_threshold))
        logger.info("Finish detect unknown events")
        return predictions

    def run(self):

        start_time = time.time()

        logger.info("Running analysis")

        if not bool(self.corpus.control_events):
            logger.warn("No control events. Nothing to do")
            return self.corpus

        if len(self.corpus.control_events) > cluster_limit:
            logger.warn("Too many potential clusters = " + str(len(self.corpus.control_events)) + " . skipping analysis")
            self.corpus.analysis_summary_message = "Too many potential clusters : " \
                                        + str(len(self.corpus.control_events)) + ".Skipping analysis!"
            return self.corpus

        vectorizer, kmeans = self.cluster_input()

        predictions = self.detect_unknown_events(vectorizer, kmeans)

        self.corpus.create_clusters(kmeans.get_clusters(), kmeans.get_centriods(),
                                    vectorizer.get_feature_names(),
                                    predictions)

        self.create_anom_clusters()

        self.detect_count_anomalies()

        self.global_score()

        self.set_xy()

        logger.info("done. time taken " + str(time.time() - start_time) + " seconds")
        print(self.corpus.score)
        return self.corpus

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
    parser.add_argument("--query", nargs='+', type=str, required=True)
    parser.add_argument("--log_collection_minute", type=int, required=True)
    parser.add_argument("--debug", required=False)

    return parser.parse_args(cli_args)


def run_debug_live_traffic(options):
    control_start = 0
    test_start = 0

    prev_out_file = None
    while control_start <= 13 or test_start < 13:

        corpus = LogCorpus()

        print(control_start, control_start)
        print(test_start, test_start)
        corpus.load_prod_file(
            '/Users/sriram_parthasarathy/wings/python/splunk_intelligence/data_prod/prodOut1.json',
            [control_start, control_start],
            [test_start, test_start], ['ip-172-31-28-126'], ['ip-172-31-19-157'], prev_out_file)

        print(options)

        if corpus.new_data:
            splunk_intel = SplunkIntelOptimized(corpus, options)
            corpus = splunk_intel.run()

            file_object = open("result.json", "w")
            file_object.write(corpus.get_output_as_json(options))
            file_object.close()
            prev_out_file = './result.json'

        control_start = control_start + 1
        test_start = test_start + 1


def run_debug_prev_run(options):
    control_start = 2
    test_start = 2
    print(options)
    prev_out_file = None
    while control_start <= 2 or test_start < 2:

        corpus = LogCorpus()

        print(control_start, control_start)
        print(test_start, test_start)
        corpus.load_prod_file_prev_run(
            '/Users/sriram_parthasarathy/wings/python/splunk_intelligence/data_prod/prev_run/control_unknown_cluster_fail.json',
            [control_start, control_start],
            '/Users/sriram_parthasarathy/wings/python/splunk_intelligence/data_prod/prev_run/test_unknown_cluster_fail.json',
            [test_start, test_start], prev_out_file)

        if corpus.new_data:
            splunk_intel = SplunkIntelOptimized(corpus, options)
            corpus = splunk_intel.run()

            file_object = open("result.json", "w")
            file_object.write(corpus.get_output_as_json(options))
            file_object.close()
            prev_out_file = './result.json'

        control_start = control_start + 1
        test_start = test_start + 1


def main(options):
    try:
        logger.info(options)
        if options.debug:
            run_debug_live_traffic(options)
            return

        corpus = LogCorpus()

        corpus.load_from_harness(options)

        if corpus.new_data == True:

            splunk_intel = SplunkIntelOptimized(corpus, options)
            corpus = splunk_intel.run()

            logger.info(corpus.save_to_harness(options.log_analysis_save_url,
                                               corpus.get_output_as_json(options), options.version_file_path,
                                               options.service_secret))
        else:
            corpus.save_to_harness(options.log_analysis_save_url,
                                   json.dumps({}), options.version_file_path,
                                   options.service_secret)

    except Exception as e:
        payload = dict(applicationId=options.appId,
                       workflowId=options.workflow_id,
                       workflowExecutionId=options.workflow_execution_id,
                       stateExecutionId=options.state_execution_id,
                       serviceId=options.service_id,
                       analysisMinute=options.log_collection_minute)
        logger.exception(e)
        raise Exception('Analysis failed for ' + json.dumps(payload))


if __name__ == "__main__":
    args = sys.argv
    logger.info(args)
    options = parse(args[1:])
    options.query = ' '.join(options.query)
    main(options)
