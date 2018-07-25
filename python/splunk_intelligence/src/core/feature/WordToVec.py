import gensim
import multiprocessing
import time
from core.feature.CustomizedTokenizer import CustomizedTokenizer
from core.util.lelogging import get_log

logger = get_log(__name__)
class WordToVec(object):
    def __init__(self, word_vec_dim=4, min_count=1, sg=1, epochs=1000):
        self.word_vec_dim = word_vec_dim
        self.min_count = min_count
        self.sg = sg
        self.workers = 1 #multiprocessing.cpu_count()
        self.epochs = epochs

    def train(self, docs_control, docs_test):
        docs = []
        for doc in docs_control:
            for i in range(1, 5):
                docs.append(doc)
        for doc in docs_test:
            for i in range(1, 5):
                docs.append(doc)

        doc_features = []
        for doc in docs:
            tokens = CustomizedTokenizer.tokenizer(doc)
            doc_features.append(tokens)
        logger.info('There are ' + str(self.workers) + ' cores, all cores will be used for training.')
        start_time = time.time()
        model = gensim.models.Word2Vec(doc_features, min_count=self.min_count, size=self.word_vec_dim, sg=self.sg, workers=self.workers)
        model.train(doc_features, total_examples=model.corpus_count, epochs=self.epochs)
        logger.info("Finished training, time taken: " + str(time.time() - start_time) + ' seconds.')
        logger.info('gensim fast version is ' + str(gensim.models.word2vec_inner.FAST_VERSION))
        return model