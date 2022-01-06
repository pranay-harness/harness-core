# Copyright 2017 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

from core.distance.SAXHMMDistance import SAXHMMDistanceFinder
from core.util.TimeSeriesUtils import MetricType
from sources.MetricTemplate import MetricTemplate

metric_template = MetricTemplate({
  "other": {
    "metricName": "other",
    "thresholds": [
      {
        "thresholdType": "ALERT_HIGHER_OR_LOWER",
        "comparisonType": "RATIO",
        "high": 1.5,
        "medium": 1.25,
        "min": 0
      },
      {
        "thresholdType": "ALERT_HIGHER_OR_LOWER",
        "comparisonType": "DELTA",
        "high": 10,
        "medium": 5,
        "min": 0
      }
    ],
    "metricType": "RESP_TIME"
  }
})

with open("resources/ts/synthetic_control.data",
          'r') as read_file:
    contents = read_file.readlines()
    groups = []
    group = []
    for row in contents:
        group.append([float(x) for x in row.split()])
        if len(group) % 100 == 0:
            groups.append(group)
            group = []

    for p in range(6):
        for q in range(p,6):
            # p = 1
            # q = 2
            errors_2 = 0
            errors_1 = 0
            for i in range(100):
                for j in range(i + 1, 100):
                    control = {'data': [groups[p][i]], 'data_type': MetricType.RESP_TIME}
                    test = {'data': [groups[q][j]], 'data_type': MetricType.RESP_TIME}
                    sdf = SAXHMMDistanceFinder('other', 3, 1, control, test, metric_template, 3)
                    result = sdf.compute_dist()
                    if result['risk'][0] == 2:
                        errors_2 += 1
                    elif result['risk'][0] == 1:
                        errors_1 += 1
            print(p, q, errors_1, errors_2)
