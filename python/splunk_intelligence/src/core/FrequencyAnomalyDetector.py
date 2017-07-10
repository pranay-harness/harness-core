import numpy as np
import logging

from ZeroDeviationClassifier import ZeroDeviationClassifier
from IsolationForestClassifier import IsolationForestClassifier
from ThreeSigmaClassifier import ThreeSigmaClassifier

logger = logging.getLogger(__name__)

class FrequencyAnomalyDetector(object):

    klassifier = {}

    def fit_transform(self, label, values):
        vals = map(int, values[:, 1])
        np_values = np.array(vals)
        mean, std = np.mean(np_values, axis=0), np.std(np_values, axis=0)
        if std < 1:
            logger.info("Using ZeroDeviationClassifier for cluster " + str(label))
            self.klassifier[label] = ZeroDeviationClassifier()
            self.klassifier[label].fit_transform(label, values, 1)
        else:
            #TODO check for Gausian distribution. If not use IRQ classifier
            logger.info("Using ThreeSigmaClassifier for cluster " + str(label))
            self.klassifier[label] = ThreeSigmaClassifier()
            self.klassifier[label].fit_transform(label, values)

    def predict(self, label, values):
        return self.klassifier[label].predict(label, values)
