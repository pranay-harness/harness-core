import argparse
import json
import sys
import time
import numpy as np
import os

from sklearn.metrics.pairwise import euclidean_distances
from core.anomaly.FrequencyAnomalyDetector import FrequencyAnomalyDetector
from core.cluster.WordToVecCluster import WordToVecCluster
from core.distance.JaccardDistance import jaccard_difference, jaccard_text_similarity
from core.feature.TFIDFVectorizer import TFIDFVectorizer
from core.feature.Tokenizer import Tokenizer
from sources.LogCorpus import LogCorpus
from core.util.lelogging import get_log
from core.feature.WordToVec import WordToVec

logger = get_log(__name__)

if os.environ.get('cluster_limit'):
    cluster_limit = os.environ.get('cluster_limit')
else:
    cluster_limit = 500

class Struct:
    def __init__(self, **entries):
        self.__dict__.update(entries)


class LogNeuralNet(object):
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
        texts = self.corpus.get_events_for_xy()
        if len(texts) != 0:
            logger.info("Create combined dist")
            min_df = 1
            max_df = 1.0
            combined_vectorizer, combined_tfidf_matrix = self.create_feature_vector(
                self.corpus.get_events_for_xy(), min_df, max_df)

            logger.info("Finish create combined dist")

            dist_matrix = combined_vectorizer.get_cosine_dist_matrix(combined_tfidf_matrix)
            self.corpus.set_xy(dist_matrix)

    def create_anom_clusters(self, wtv_model):

        logger.info("Create anomalous clusters")

        unknown_anomalies_text = self.corpus.get_unknown_anomalies_text()

        if len(unknown_anomalies_text) > 0:


            anom_cluster = WordToVecCluster(wtv_model, self._options.sim_threshold, np.array(unknown_anomalies_text))
            anom_cluster.cluster()

            self.corpus.create_anom_clusters(anom_cluster.get_clusters())

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
        docs_control = self.corpus.get_control_events_text_as_np()
        docs_test = self.corpus.get_test_events_text_as_np()
        all_docs = np.concatenate([docs_control, docs_test])
        max_df = 1.0 #0.99 if len(self.corpus.get_control_events_text_as_np()) > 1 else 1.0
        logger.info("setting min_df = " + str(min_df) + " and max_df = " + str(max_df))
        logger.info("Start vectorization....")
        vectorizer, tfidf_feature_matrix = self.create_feature_vector(all_docs, min_df, max_df)
        wtv_model = WordToVec().train(docs_control, docs_test)
        wtv_cluster = WordToVecCluster(wtv_model, self._options.sim_threshold, docs_control)
        start_time = time.time()
        wtv_cluster.cluster()

        logger.info("Finished clustering controls, time taken: " + str(time.time() - start_time) + ' seconds.')

        return wtv_cluster, wtv_model, tfidf_feature_matrix

    def detect_count_anomalies(self):

        logger.info("Detect Count Anomalies....")

        control_clusters = self.corpus.get_control_clusters()
        test_clusters = self.corpus.get_test_clusters()

        classifier = FrequencyAnomalyDetector()

        for idx, group in test_clusters.items():
            values = []
            for host, data in control_clusters[idx].items():
                values.extend(np.array([freq.get('count') for freq in data.get('message_frequencies')]))
            values_control = np.column_stack(([idx] * len(values), values))
            classifier.fit_transform(idx, values_control)
            max_score = 0.0
            for host, data in group.items():
                values_test = np.array([freq.get('count') for freq in data.get('message_frequencies')])
                anomalous_counts, score = classifier.predict(idx,
                                                             np.column_stack(([idx] * len(values_test), values_test)))
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




    def detect_unknown_events(self, wtv_cluster, tfidf_feature_matrix):
        logger.info("Detect unknown events")
        start_time = time.time()
        predictions = []
        if bool(self.corpus.test_events):

            predictions = np.array(
                wtv_cluster.detect_anomaly_text_diff(self.corpus.get_control_events_text_as_np(),
                                                     self.corpus.get_test_events_text_as_np(), tfidf_feature_matrix))
        logger.info("Finished detecting unknown events, time taken: " + str(time.time() - start_time) + ' seconds.')
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

        wtv_cluster, wtv_model, tfidf_feature_matrix = self.cluster_input()

        predictions = self.detect_unknown_events(wtv_cluster, tfidf_feature_matrix)

        self.corpus.create_clusters(wtv_cluster.get_clusters(), [],
                                    [],
                                    predictions)

        self.create_anom_clusters(wtv_model)

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



def main(options):
    try:
        logger.info(options)

        corpus = LogCorpus()

        corpus.load_from_harness(options)

        if corpus.new_data == True:

            lnn = LogNeuralNet(corpus, options)
            corpus = lnn.run()

            logger.info(corpus.save_to_harness(options.log_analysis_save_url,
                                               corpus.get_output_as_json(options), options.version_file_path,
                                               options.service_secret))
        else:
            corpus.save_to_harness(options.log_analysis_save_url,
                                   {}, options.version_file_path,
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
    options_dict = json.loads(args[1])
    options = Struct(**options_dict)
    main(options)