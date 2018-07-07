import numpy as np
import multiprocessing

from core.util.lelogging import get_log
from sklearn.metrics.pairwise import cosine_similarity
from core.distance.JaccardDistance import jaccard_pairwise_difference
from core.feature.CustomizedTokenizer import CustomizedTokenizer

"""
Wrapper class for wordtovec clustering
"""

logger = get_log(__name__)

class WordToVecCluster(object):

    def __init__(self, wtv_model, threshold, docs):
        self.threshold = threshold
        self.wtv_model = wtv_model
        self.docs = docs.tolist()
        self.control_labels = np.array([-1] * len(docs))

    def cluster(self):
        workers = min(multiprocessing.cpu_count(), len(self.docs))
        docs_batch = [[] for i in range(workers)]
        indices = [[] for i in range(workers)]
        jobs = []
        for idx, doc in enumerate(self.docs):
            docs_batch[idx % workers].append(doc)
            indices[idx % workers].append(idx)
        queue = multiprocessing.Queue()
        job_id = 0
        for i in range(workers):
            p = multiprocessing.Process(target=self.parallel_cluster,
                                        args=(queue, docs_batch[i], indices[i]))
            job_id = job_id + 1
            jobs.append(p)
            p.start()

        processed = 0
        while processed < len(jobs):
            try:
                val = queue.get(timeout=240)
            except Empty:
                raise Exception("Timeout occured in one of the clustering worker process. Check logs")
            self.control_labels[val[1]] = val[0]
            processed = processed + 1
        # merging
        joined_indices = []
        for idx, r_indices in enumerate(indices):
            if idx != 0:
                self.merge_clusters(joined_indices, r_indices)
            joined_indices.extend(r_indices)

    def parallel_cluster(self, queue, doc_batch, indices):
        queue.put(self.find_cluster(doc_batch, indices))

    def merge_clusters(self, left_indices, right_indices):
        labels = set(self.control_labels[left_indices].tolist())
        visited = np.array([-1] * len(right_indices))
        right_lables = self.control_labels[right_indices]
        for i, r_doc_idx in enumerate(right_indices):
            if visited[i] == -1:
                ## mark all with this label as visited
                old_label_right = right_lables[i]
                visited[right_lables == old_label_right] = 1
                for l in labels:
                    c_ind = np.where(self.control_labels == l)[0][0]
                    score = self.text_diff_score(self.docs[c_ind], self.docs[r_doc_idx])
                    if score >= self.threshold:
                        # update labels
                        self.control_labels[self.control_labels == old_label_right] = l
                        break

    def find_cluster(self, docs, indices):

        control_labels = [-1] * len(docs)
        # the first label is the first index
        labels = []
        labels.append(indices[0])
        control_labels[0] = labels[0]
        # if there is just one doc, there is just one cluster label that is already set in control_labels, so do
        # followings when there are more than one doc
        if len(control_labels) > 1:
            for ind in range(1, len(control_labels)):
                found_cluster = False
                # compare with all already-built clusters to see to which one the new doc belongs
                for l in labels:
                    c_ind = control_labels.index(l)
                    score = self.text_diff_score(docs[c_ind], docs[ind])
                    if score >= self.threshold:
                        control_labels[ind] = l
                        found_cluster = True
                        break
                if not (found_cluster):
                    new_label = indices[ind]
                    labels.append(new_label)
                    control_labels[ind] = new_label
        return control_labels, indices

    def get_clusters(self):
        """

        :return: the cluster assignments
        """
        return self.control_labels.tolist()

    def get_num_clusters(self):
        """

        :return: the total number of clusters
        """
        return len(set(self.control_labels.tolist()))

    def detect_anomaly_text_diff(self, docs_control, docs_test, tfidf_feature_matrix):
        control_tfid_mat = tfidf_feature_matrix[:len(docs_control),:]
        control_labels = self.get_clusters()
        workers = min(multiprocessing.cpu_count(), len(docs_test))
        test_docs_batch = [[] for i in range(workers)]
        indices = [[] for i in range(workers)]
        results = np.array([{}] * len(docs_test))
        jobs = []
        for idx, doc in enumerate(docs_test):
            test_docs_batch[idx % workers].append(doc)
            indices[idx % workers].append(idx)
        queue = multiprocessing.Queue()
        job_id = 0
        for i in range(workers):
            test_tfid_mat = tfidf_feature_matrix[(len(docs_control)+np.array(indices[i])).tolist(),:]
            p = multiprocessing.Process(target=self.parallel_find_best_match,
                                        args=(queue, docs_control,test_docs_batch[i],control_labels, control_tfid_mat, test_tfid_mat, indices[i]))
            job_id = job_id + 1
            jobs.append(p)
            p.start()

        processed = 0
        while processed < len(jobs):
            try:
                val = queue.get(timeout=240)
            except Empty:
                raise Exception("Timeout occured in one of the clustering worker process. Check logs")
            results[val[1]] = val[0]
            processed = processed + 1
        return results.tolist()

    def parallel_find_best_match(self, queue, docs_control, docs_test, control_labels, control_tfid_mat, test_tfid_mat, indices):
        queue.put(self.find_best_match(docs_control, docs_test, control_labels, control_tfid_mat, test_tfid_mat, indices))

    def find_best_match(self, docs_control, docs_test, control_labels, control_tfid_mat, test_tfid_mat, indices):


        labels = set(control_labels)
        results = []

        for test_idx, test_doc in enumerate(docs_test):
            best_score = -2
            anomaly = -1
            for l in labels:
                c_ind = control_labels.index(l)
                score = self.text_diff_score(docs_control[c_ind], test_doc)
                if score >= self.threshold:
                    anomaly = 1
                    best_score = score
                    best_label = l
                    break
                if score >= best_score:
                    best_score = score
                    best_label = l
            if anomaly == -1:
                sim = cosine_similarity(test_tfid_mat[test_idx, :], control_tfid_mat)
                idx1, idx2 = np.where(sim > 0.93) # tfidf threshold
                # if there exist a good match in control based on cosine sim of tfidf vecs, it should not be anomaly
                if len(idx1) != 0:
                    match_idx = np.argmax(sim)
                    best_label = control_labels[match_idx]
                    best_score = sim[0, match_idx]
                    anomaly = 1
            results.append(
                dict(cluster_label=int(best_label), anomaly=anomaly, cluster_score=1,
                     score=float(best_score)))

        return results, indices

    def text_diff_score(self, control_doc, test_doc):
        test_doc_tokens = CustomizedTokenizer.tokenizer(test_doc)
        control_doc_tokens = CustomizedTokenizer.tokenizer(control_doc)
        control_diff, test_diff = jaccard_pairwise_difference(control_doc_tokens, test_doc_tokens)
        if len(control_diff) == 1 and len(test_diff) == 1:
            score = self.threshold + 0.01
        else:
            control_diff_vec = []
            test_diff_vec = []
            word_vec_dim = self.wtv_model.vector_size
            for i in range(max(len(control_diff), len(test_diff))):
                if i < len(control_diff) and control_diff[i] != ' ':
                    control_diff_vec.extend(self.wtv_model.wv.word_vec(control_diff[i].lower()))
                else:
                    control_diff_vec.extend(word_vec_dim * [0])

                if i < len(test_diff) and test_diff[i] != ' ':
                    test_diff_vec.extend(self.wtv_model.wv.word_vec(test_diff[i].lower()))
                else:
                    test_diff_vec.extend(word_vec_dim * [0])
            if len(control_diff_vec) == 0:
                score = 1
            else:
                score = cosine_similarity(self.vec2matrix(np.array(control_diff_vec)),
                                          self.vec2matrix(np.array(test_diff_vec)))[0][0]
        return score

    def vec2matrix(self, mat):
        if mat.ndim < 2:
            mat = mat.reshape(1, -1)
        return mat