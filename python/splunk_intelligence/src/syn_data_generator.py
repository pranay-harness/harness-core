# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import numpy as np
from core.distance.SAXHMMDistance import SAXHMMDistanceFinder
from core.util.TimeSeriesUtils import MetricType
from scipy.stats import truncnorm
from sources.MetricTemplate import MetricTemplate


def normal(n_samples=100,t_samples=30, m=50, a=3):
    data = []
    for i in range(n_samples):

        m = np.random.randint(1, a,1)
        sample = m +  np.random.normal(0, 1 ,t_samples)
        data.append(sample)
    return data


def cyclic(n_samples=100,t_samples=30, T_l=1, h_T = 3):

    data = []

    for i in range(n_samples):
        normal_data = normal(n_samples=1, t_samples=30, m=50, a=3)[0]
        # period can be anything between 10 min to 20 min given 0 to 30 timestamps are in min
        T = 10 * truncnorm.rvs(1, 2, size=1)
        # amplitutde of sin
        amp = 40 * truncnorm.rvs(1, 2, size=1)
        sample = normal_data + amp * np.cos((2. * 3.14 / (T))* np.arange(0, t_samples))
        data.append(sample)
    return data

def increasing(n_samples=100,t_samples=30, m=50, a=-3, b=3):
    data = []
    for i in range(n_samples):
        normal_data = normal(n_samples=1, t_samples=1, m=50, a=3)[0]
        # slope between 2 and 3
        slope = a * truncnorm.rvs(1, 2, size=1)
        sample = normal_data + slope * np.arange(0, t_samples)
        data.append(sample)
    return data

def decreasing(n_samples=100,t_samples=30, m=50, a=-3, b=3):
    data = []
    for i in range(n_samples):
        normal_data = normal(n_samples=1, t_samples=1, m=50, a=3)[0]
        # slope between 3 and 4.5
        slope =  a *truncnorm.rvs(1, 2, size=1)
        sample = normal_data - slope * np.arange(0, t_samples)
        data.append(sample)
    return data

def upward_shifts(n_samples=100,t_samples=30, m=50, a=-3, b=3):
    data = []
    for i in range(n_samples):
        sample = np.zeros(t_samples)
        step = np.random.randint(t_samples/3, 2.0 * t_samples/3)

        sample = normal(n_samples=1, t_samples=t_samples, m=50, a=3)[0]
        # slope between 2 and 3
        x = normal(n_samples=1, t_samples=t_samples - step, m=7.5, a=10)[0]
        sample [step:] = sample [step:] + x
        data.append(sample)
    return data

def downward_shifts(n_samples=100,t_samples=30, m=50, a=-3, b=3):
    data = []
    for i in range(n_samples):
        sample = np.zeros(t_samples)
        step = np.random.randint(t_samples/3, 2.0 * t_samples/3)

        sample = normal(n_samples=1, t_samples=t_samples, m=50, a=3)[0]
        # slope between 2 and 3
        x = normal(n_samples=1, t_samples=t_samples - step, m=7.5, a=10)[0]
        sample [step:] = sample [step:] - x
        data.append(sample)
    return data

metric_template = MetricTemplate(
{
"default": {
  "other": {
    "metricName": "other",
    "thresholds": [
      {
        "thresholdType": "ALERT_HIGHER_OR_LOWER",
        "comparisonType": "RATIO",
        "ml": 0
      },
      {
        "thresholdType": "ALERT_HIGHER_OR_LOWER",
        "comparisonType": "DELTA",
        "ml": 0
      }
    ],
    "metricType": "RESP_TIME"
  }
}
})


n_samples=30

for a in range(2,30, 1):
    data = np.array(normal(n_samples=n_samples, a=a))
    errors_2 = 0
    errors_1 = 0

    for i in range(n_samples):
        test = {'data': [data[i,:]], 'data_type': MetricType.RESP_TIME}

        #indices = np.random.randint(0, n_samples, 1)
        for id in range(n_samples):


            control_data = [data[id,:]]
            control = {'data': control_data, 'data_type': MetricType.RESP_TIME}
            window = 3
            sdf = SAXHMMDistanceFinder('other', 'other', window, 1, control, test, metric_template, 1)
            result = sdf.compute_dist()
            if result['risk'][0] == 2:
                errors_2 += 1
            elif result['risk'][0] == 1:
                errors_1 += 1
    print(a, (errors_1 + errors_2)/900.)

#np.savetxt('/Users/parnianzargham/Desktop/portal/python/splunk_intelligence/tests/resources/ts/syn_data.txt', all)
