# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import pandas as pd
import plotly as py
from plotly import tools
from plotly.graph_objs import Scatter, Scatter3d, Layout, Figure, Marker, Histogram
from random import randint


def split(input, length, size):
    input.replace('\n', ' ')
    input.replace('\tat', ' ')
    return '<br>'.join([input[start:start + size] for start in range(0, length, size)])


def hist_plot(control_x, test_x):
    data = [Histogram(x=control_x,
    opacity=0.75),
            Histogram(x=test_x,
                      opacity=0.75)
            ]

    layout = Layout(hovermode='closest')

    figure = Figure(data=data, layout=layout)

    py.offline.iplot(figure)

# TODO tooltips should be plain text
def scatter_plot(xy_matrix, tooltips):
    data = [Scatter(x=xy_matrix[:, 0],
                    y=xy_matrix[:, 1],
                    text=[split(s[0], min(100, len(s[0])), 100) + '<br> id = ' + str(s[1]) for s in tooltips],
                    mode='markers')]

    layout = Layout(hovermode='closest')

    figure = Figure(data=data, layout=layout)

    py.offline.iplot(figure)


def scatter_plot_groups(xy_matrix, labels, tooltips, legends=None, cc=None, sizes=None):
    if cc is None:
        cc = ["'rgb " + str(randint(0, 255)) + "," + str(randint(0, 255)) + "," + str(randint(0, 255)) + "'" for c in
              range(len(set(labels)))]

    if sizes is None:
        sizes = [10 for i in range(xy_matrix.shape[0])]

    # create data frame that has the result of the MDS plus the cluster numbers and titles
    df = pd.DataFrame(dict(x=xy_matrix[:, 0], y=xy_matrix[:, 1], label=labels, tooltip=tooltips))

    # group by cluster
    groups = df.groupby('label')

    py.offline.iplot({
        'data': [
            Scatter(x=group.x,
                    y=group.y,
                    # text=[ str[0:100] for str in group.title],
                    text=[split(s[0], min(100, len(s[0])), 100) + '<br> id = ' + str(s[1]) for s in group.tooltip],
                    mode='markers',
                    name=legends[name],
                    marker=Marker(color=cc[name], size=sizes, opacity=0.3)) for name, group in groups
        ],
        'layout': Layout(hovermode='closest')
    }, show_link=False, filename='123')


def scatter_plot_groups_4d(xy_matrix, labels, clusters, tooltips, cc=None):
    if cc is None:
        cc = ["'rgb " + str(randint(0, 255)) + "," + str(randint(0, 255)) + "," + str(randint(0, 255)) + "'" for c in
              range(len(set(labels)))]

    # create data frame that has the result of the MDS plus the cluster numbers and titles
    df = pd.DataFrame(dict(x=xy_matrix[:, 0], y=xy_matrix[:, 1], z=xy_matrix[:, 2],
                           label=labels, cluster=clusters, tooltip=tooltips))

    # group by cluster
    data = []
    # group by cluster
    groups = df.groupby('cluster')

    for id, clusters in groups:
        groups = clusters.groupby('label')
        for name, group in groups:
            data.append(Scatter3d(x=group.x,
                                  y=group.y,
                                  z=group.z,
                                  # text=[ str[0:100] for str in group.title],
                                  legendgroup='cluster - ' + str(id),
                                  name='cluster - ' + str(id),
                                  text=[split(s[0], min(100, len(s[0])), 100) + '<br> id = ' + str(s[1]) for s in group.tooltip],
                                  mode='markers',
                                  marker=Marker(color=cc[name], opacity=0.5, size=(name + 1) + 5)))
    py.offline.iplot({
        'data': data,
        'layout': Layout(hovermode='closest')
    }, show_link=False, filename='123')


def get_scatter_trace(xy_matrix, labels, tooltips, legends, cc ):
    colors = [cc[label] for label in labels]
    all_legends = [legends[label] for label in labels]
    sizes = [10 for i in range(xy_matrix.shape[0])]
    # if fig is None:
    #     fig = tools.make_subplots(rows=1, cols=2, subplot_titles=('A', 'B'))
    text = [split(s[0], min(100, len(s[0])), 100) + '<br> id = ' + str(s[1]) for s in tooltips]
    trace = Scatter(x=xy_matrix[:, 0], y=xy_matrix[:, 1], text=text, mode='markers', name ='ll', marker=Marker(color=colors, size=sizes), showlegend=True)
    return trace


def plot_AB(traces, subtitle_names):
    fig = tools.make_subplots(rows=1, cols=2, subplot_titles=subtitle_names)
    fig.append_trace(traces[0], 1, 1)
    fig.append_trace(traces[1], 1, 2)
    fig['layout'].update(showlegend=False)
    py.offline.iplot(fig, filename='123')
