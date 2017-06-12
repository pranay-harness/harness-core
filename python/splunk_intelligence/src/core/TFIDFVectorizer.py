from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.manifold import MDS
from sklearn.metrics.pairwise import cosine_similarity


"""
Vectorizer based on TF-IDF
"""
class TFIDFVectorizer(object):

    def __init__(self, tokenizer, min_df, max_df):
        """

        :param tokenizer: tokenizer to extract tokens
        :param min_df: min document frequency
        :param max_df: max document frequency
        """
        self.tokenizer = tokenizer
        self.min_df = min_df
        self.max_df = max_df
        self.tfidf_vectorizer = TfidfVectorizer(max_features=200000,
                                                min_df=self.min_df,
                                                max_df=self.max_df,
                                                stop_words='english',
                                                use_idf=True, tokenizer=self.tokenizer, ngram_range=(1, 1))

    def fit_transform(self, texts):
        """

        :param texts: the input texts
        :return: the tfidf feature matrix
        """
        return self.tfidf_vectorizer.fit_transform(texts)

    def get_feature_names(self):
        """

        :return: the feature names
        """
        return self.tfidf_vectorizer.get_feature_names()

    def transform(self, texts):
        """

        :param texts: inupt texts
        :return: return the feature matrix based on the existing
                features extracted using fit_transform
        """
        return self.tfidf_vectorizer.transform(texts)

    def get_cosine_dist_matrix(self, tfidf_matrix):
        """

        :param tfidf_matrix: the tfid feature matrix
        :return: the cosine distance
        """
        dist = 1 - cosine_similarity(tfidf_matrix)

        mds = MDS(n_components=2, dissimilarity="precomputed", random_state=1)

        pos = mds.fit_transform(dist)  # shape (n_components, n_samples)

        return pos
