# Copyright 2018 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import numpy as np

from core.classifier.ThreeSigmaClassifier import ThreeSigmaClassifier
from core.classifier.ZeroDeviationClassifier import ZeroDeviationClassifier
from core.util.lelogging import get_log

logger = get_log(__name__)

class FrequencyAnomalyDetector(object):

    klassifier = {}

    def fit_transform(self, label, values):
        vals = map(int, values[:, 1])
        np_values = np.array(vals)
        mean, std = np.mean(np_values, axis=0), np.std(np_values, axis=0)
        if std < 0.1:
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



# x = [2,2,1,1]
# y = [2,2,1]
#
#
# x = [[1,i] for i in x]
#
# y = [[1,i] for i in y]
#
#
# det = FrequencyAnomalyDetector()
# det.fit_transform(1, np.array(x))
# print(det.predict(1, np.array(y)))
