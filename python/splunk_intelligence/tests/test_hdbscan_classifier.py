from core.HDBScanClassifier import HDBScanClassifier
import numpy as np


def test_anomaly():
    control_data = []
    for x in [u'292', u'1561', u'1686', u'2385', u'257', u'3225', u'352', u'4210',
              u'5396', u'8086', u'8368', u'931']:
        control_data.append([1, x])

    test_data = []
    for x in [300, 500, 6, 5, 4, 7800, 8450]:
        test_data.append([1, x])

    hdbscanKlassifier = HDBScanClassifier()
    hdbscanKlassifier.fit_transform(1, control_data)

    predictions, score = hdbscanKlassifier.predict(1, test_data)
    assert abs(score - 0.42857142857142855) < 0.00001
    np.testing.assert_array_equal(predictions.tolist(), [1, -1, -1, -1, -1, 1, 1])


def test_anomaly_1():
    control_data = [[712., 784., 805., 740., 767., 863., 808., 763., 774.],
                    [715., 780., 804., 740., 770., 875., 802., 756., 765.],
                    [721., 781., 806., 752., 764., 870., 819., 764., 770.]]

    test_data = [[663., 363., 380., 351., 434., 0., 259., 518., 399.]]

    hdbscanKlassifier = HDBScanClassifier()
    hdbscanKlassifier.fit_transform(1, control_data)

    predictions, score = hdbscanKlassifier.predict(1, test_data)
    assert abs(score - 0.0) < 0.00001
    np.testing.assert_array_equal(predictions.tolist(), [-1])


def test_fit():
    control_data = [
        [7., 1., 1., 1., 5., 16., 5., 3., 2.],
        [12., 2., 1., 0., 1., 14., 9., 3., 2.],
        [8., 0., 0., 0., 2., 11., 5., 5., 2.]]

    test_data = [[11., 0., 0., 0., 4., 0., 0., 8., 0.]]

    hdbscanKlassifier = HDBScanClassifier()
    hdbscanKlassifier.fit_transform(1, control_data)

    predictions, score = hdbscanKlassifier.predict(1, test_data)
    assert abs(score - 0.0) < 0.00001
    np.testing.assert_array_equal(predictions.tolist(), [-1])
