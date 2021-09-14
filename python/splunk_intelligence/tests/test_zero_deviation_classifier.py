# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0

import numpy as np

from core.classifier.ZeroDeviationClassifier import ZeroDeviationClassifier


def test_fit():
    zdc = ZeroDeviationClassifier()
    zdc.fit_transform(1, np.array([[1, 7],
                                   [1, 7],
                                   [1, 7]]), 0.2)
    predictions, score = zdc.predict(1, np.array([[1, 8]]))
    assert score == 1
    assert len(np.where(predictions == -1)[0]) == 0


def test_fit():
    zdc = ZeroDeviationClassifier()
    zdc.fit_transform(1, np.array([[1, 7],
                                   [1, 7],
                                   [1, 7]]), 0.3)
    predictions, score = zdc.predict(1, np.array([[1, 8]]))
    assert score == 1
    assert len(np.where(predictions == -1)[0]) == 0


def test_fit_tolerant():
    zdc = ZeroDeviationClassifier()
    zdc.fit_transform(1, np.array([[1, 7],
                                   [1, 7],
                                   [1, 7]]), 0.05)
    predictions, score = zdc.predict(1, np.array([[1, 12]]))
    assert score == 1
    assert len(np.where(predictions == -1)[0]) == 0


def test_fit_anomaly():
    zdc = ZeroDeviationClassifier()
    zdc.fit_transform(1, np.array([[1, 7],
                                   [1, 7],
                                   [1, 7]]), 0.05)
    predictions, score = zdc.predict(1, np.array([[1, 13]]))
    assert score == 0
    assert len(np.where(predictions == -1)[0]) == 1
