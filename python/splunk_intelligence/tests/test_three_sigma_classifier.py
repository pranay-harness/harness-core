# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0

import numpy as np

from core.classifier.ThreeSigmaClassifier import ThreeSigmaClassifier


def test_fit():
    zdc = ThreeSigmaClassifier()
    zdc.fit_transform(1, np.array([[1, 7],
                                   [1, 7],
                                   [1, 7]]))
    predictions, score = zdc.predict(1, np.array([[1, 8]]))
    assert score == 1
    assert len(np.where(predictions == -1)[0]) == 0


def test_fit():
    zdc = ThreeSigmaClassifier()
    zdc.fit_transform(1, np.array([[1, 7],
                                   [1, 7],
                                   [1, 7]]))
    predictions, score = zdc.predict(1, np.array([[1, 8]]))
    assert score == 1
    assert len(np.where(predictions == -1)[0]) == 0


def test_fit_tolerant():
    zdc = ThreeSigmaClassifier()
    zdc.fit_transform(1, np.array([[1, 7],
                                   [1, 7],
                                   [1, 7]]))
    predictions, score = zdc.predict(1, np.array([[1, 12]]))
    assert score == 1
    assert len(np.where(predictions == -1)[0]) == 0


def test_fit_anomaly():
    zdc = ThreeSigmaClassifier()
    zdc.fit_transform(1, np.array([[1, 7],
                                   [1, 7],
                                   [1, 7]]))
    predictions, score = zdc.predict(1, np.array([[1, 13]]))
    assert score == 0
    assert len(np.where(predictions == -1)[0]) == 1


def test_fit():
    zdc = ThreeSigmaClassifier()
    zdc.fit_transform(1, np.array([[1, 1],
                                   [1, 2],
                                   [1, 4],
                                   [1,9]]))
    predictions, score = zdc.predict(1, np.array([[1, 12], [1,12]]))
    assert score == 1
    assert len(np.where(predictions == -1)[0]) == 0


def test_fit():
    zdc = ThreeSigmaClassifier()
    zdc.fit_transform(1, np.array([[1, 1],
                                   [1, 1],
                                   [1, 1],
                                   [1,2]]))
    predictions, score = zdc.predict(1, np.array([[1, 3], [1,3]]))
    assert score == 1
    assert len(np.where(predictions == -1)[0]) == 0


def test_fit_anomaly_1():

    zdc = ThreeSigmaClassifier()
    zdc.fit_transform(1, np.array([[1, 1],
                               [1, 1],
                               [1, 1],
                               [1, 2]]))
    predictions, score = zdc.predict(1, np.array([[1, 8], [1, 8]]))
    assert score == 0
    assert len(np.where(predictions == -1)[0]) == 2


def test_fit_anomaly_2():

    zdc = ThreeSigmaClassifier()
    zdc.fit_transform(1, np.array([[1, 1],
                               [1, 1],
                               [1, 1],
                               [1, 2]]))
    predictions, score = zdc.predict(1, np.array([[1, 7], [1, 8]]))
    assert score == 0.5
    assert len(np.where(predictions == -1)[0]) == 1
