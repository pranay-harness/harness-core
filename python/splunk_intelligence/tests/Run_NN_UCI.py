from core.distance.SAXHMMDistance import SAXHMMDistanceFinder
from core.util.TimeSeriesUtils import MetricType
from sources.MetricTemplate import MetricTemplate
from random import randint

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

with open("resources/ts/syn_data.txt",
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
        for q in range(p, 6):
            # p = 1
            # q = 2
            errors_2 = 0
            errors_1 = 0
            for i in range(100):
                test = {'data': [groups[p][i]], 'data_type': MetricType.RESP_TIME}
                control_data = []
                list_s = []
                for k in range(5):
                    s = randint(0, 99)
                    list_s.append(s)
                    control_data.append(groups[q][s])
                control = {'data': control_data, 'data_type': MetricType.RESP_TIME}
                window = 1 if p == 1 or q==1 else 3
                sdf = SAXHMMDistanceFinder('other', 'other', window, 1, control, test, metric_template, 1)
                result = sdf.compute_dist()
                if result['risk'][0] == 2:
                    errors_2 += 1
                elif result['risk'][0] == 1:
                    errors_1 += 1
            print(p, q, errors_1, errors_2)

    # for p in range(6):
    #     for q in range(p,6):
    #         # p = 1
    #         # q = 2
    #         errors_2 = 0
    #         errors_1 = 0
    #         for i in range(100):
    #             for j in range(i + 1, 100):
    #                 control = {'data': [groups[p][i]], 'data_type': MetricType.RESP_TIME}
    #                 test = {'data': [groups[q][j]], 'data_type': MetricType.RESP_TIME}
    #                 sdf = SAXHMMDistanceFinder('other', 'other', 3, 1, control, test, metric_template, 1)
    #                 result = sdf.compute_dist()
    #                 if result['risk'][0] == 2:
    #                 elif result['risk'][0] == 1:
    #                     errors_1 += 1
    #         print(p, q, errors_1, errors_2)