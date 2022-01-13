# Copyright 2017 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import numpy as np

from core.classifier.SimpleDistanceClassifier import SimpleDistanceClassifier


def test_fit():
    spc = SimpleDistanceClassifier()
    spc.fit_transform(1, np.array([[7., 1., 1., 1., 5., 16., 5., 3., 2],
                                   [8., 0., 0., 0., 2., 11., 5., 5., 2],
                                   [11., 0., 0., 0., 4., 0., 0., 8., 0]]), 0.2)
    predictions, score = spc.predict(1, np.array([[12., 2., 1., 0., 1., 14., 9., 3., 2]]))
    assert len(np.where(predictions == -1)[0]) == 0
    assert score == 1


def test_anomalous():
    spc = SimpleDistanceClassifier()
    spc.fit_transform(1, np.array([[7., 1., 1., 1., 5., 16., 5., 3., 2],
                                   [8., 0., 0., 0., 2., 11., 5., 5., 2],
                                   [11., 0., 0., 0., 4., 0., 0., 8., 0]]), 0.2)
    predictions, score = spc.predict(1, np.array([[12., 2., 1., 0., 1., 14., 9., 3., 2],
                                                  [22., 22., 21., 20., 21., 24., 29., 23., 22]]))
    assert len(np.where(predictions == -1)[0]) == 1
    assert score == 0.5
