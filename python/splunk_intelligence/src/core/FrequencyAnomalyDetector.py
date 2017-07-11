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
            # Anything greater than 25% will be an anomaly
            self.klassifier[label].fit_transform(label, values, .25)
        else:
            #TODO check for Gausian distribution. If not use IRQ classifier
            logger.info("Using ThreeSigmaClassifier for cluster " + str(label))
            self.klassifier[label] = ThreeSigmaClassifier()
            #self.klassifier[label] = IsolationForestClassifier()

            self.klassifier[label].fit_transform(label, values)

    def predict(self, label, values):
        return self.klassifier[label].predict(label, values)



x = [47, 49, 52, 51, 51, 50, 53, 50, 50, 51, 51, 51,
 52, 51, 51, 50, 47, 49, 51, 51, 51, 50, 52, 50, 50,
 51, 51, 52, 52, 51, 50, 51, 47, 49, 51, 51, 51, 51, 52, 50,
 51, 52, 51, 52, 52, 51, 51, 50, 48, 49, 51, 51, 51, 50, 53,
 50, 50, 51, 51, 51, 52, 51, 50, 51]


x = [[1,i] for i in x]

y = [41, 43 , 45 , 44 , 45 , 44 , 37 , 45 , 44 , 45 , 45 , 45,  46, 45, 45, 44]

y = [[1,i] for i in y]














