from core.distance.SAXHMMDistance import SAXHMMDistanceFinder
from core.util.TimeSeriesUtils import MetricToDeviationType, MetricType

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
                    control = {'data': [groups[p][i]], 'data_type': MetricType.TIME}
                    test = {'data': [groups[q][j]], 'data_type': MetricType.TIME}
                    sdf = SAXHMMDistanceFinder('other', 3, 1, control, test, MetricToDeviationType.BOTH, 0, 3)
                    result = sdf.compute_dist()
                    if result['risk'][0] == 2:
                        errors_2 += 1
                    elif result['risk'][0] == 1:
                        errors_1 += 1
            print(p, q, errors_1, errors_2)
