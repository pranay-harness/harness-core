# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0

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
