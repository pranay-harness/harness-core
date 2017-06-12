from random import randint

import pandas as pd
import plotly as py
from plotly.graph_objs import Scatter, Scatter3d, Layout, Figure, Marker


def split(input, length, size):
    input.replace('\n', ' ')
    input.replace('\tat', ' ')
    return '<br>'.join([input[start:start + size] for start in range(0, length, size)])


def scatter_plot(xy_matrix, tooltips):
    data = [Scatter(x=xy_matrix[:, 0],
                    y=xy_matrix[:, 1],
                    text=[split(s[0], min(100, len(s[0])), 100) + '<br> id = ' + str(s[1]) for s in tooltips],
                    mode='markers')]

    layout = Layout(hovermode='closest')

    figure = Figure(data=data, layout=layout)

    py.offline.iplot(figure)

def line_plot(xy_matrix):
    data = [Scatter(x=xy_matrix[:, 0],
                    y=xy_matrix[:, 1],
                    mode='lines+markers')]

    layout = Layout(hovermode='closest')

    figure = Figure(data=data, layout=layout)

    py.offline.iplot(figure)

def scatter_plot_groups(xy_matrix, labels, tooltips, cc = None):

    if cc is None:
        cc = ["'rgb " + str(randint(0, 255)) + "," + str(randint(0, 255)) + "," + str(randint(0, 255)) + "'" for c in
            range(len(set(labels)))]

    # tooltip per distinct label
    if len(tooltips) == len(set(labels)):

        # create data frame that has the result of the MDS plus the cluster numbers and titles
        df = pd.DataFrame(dict(x=xy_matrix[:, 0], y=xy_matrix[:, 1], label=labels))

        # group by cluster
        groups = df.groupby('label')
    
        py.offline.iplot({
            'data': [
                Scatter(x=group.x,
                        y=group.y,
                        # text=[ str[0:100] for str in group.title],
                        text=tooltips[name],
                        mode='markers',
                        marker=Marker(color=cc[name])) for name, group in groups
            ],
            'layout': Layout(hovermode='closest')
        }, show_link=False, filename='123')
        
    elif len(tooltips) == len(labels):
        
        # create data frame that has the result of the MDS plus the cluster numbers and titles
        df = pd.DataFrame(dict(x=xy_matrix[:, 0], y=xy_matrix[:, 1], label=labels, tooltip=tooltips))

        # group by cluster
        groups = df.groupby('label')
        
        py.offline.iplot({
            'data': [
                Scatter(x=group.x,
                        y=group.y,
                        # text=[ str[0:100] for str in group.title],
                        text=group.tooltip,
                        mode='markers',
                        marker=Marker(color=cc[name])) for name, group in groups
            ],
            'layout': Layout(hovermode='closest')
        }, show_link=False, filename='123')
        
def scatter_plot_groups_3d(xy_matrix, labels, tooltips, cc = None):

    if cc is None:
        cc = ["'rgb " + str(randint(0, 255)) + "," + str(randint(0, 255)) + "," + str(randint(0, 255)) + "'" for c in
            range(len(set(labels)))]

    # tooltip per distinct label
    if len(tooltips) == len(set(labels)):

        # create data frame that has the result of the MDS plus the cluster numbers and titles
        df = pd.DataFrame(dict(x=xy_matrix[:, 0], y=xy_matrix[:, 1], label=labels))

        # group by cluster
        groups = df.groupby('label')
    
        py.offline.iplot({
            'data': [
                Scatter3d(x=group.x,
                        y=group.y,
                        z=group.z,
                        # text=[ str[0:100] for str in group.title],
                        text=tooltips[name],
                        mode='markers',
                        marker=Marker(color=cc[name])) for name, group in groups
            ],
            'layout': Layout(hovermode='closest')
        }, show_link=False, filename='123')
        
    elif len(tooltips) == len(labels):

        # create data frame that has the result of the MDS plus the cluster numbers and titles
        df = pd.DataFrame(dict(x=xy_matrix[:, 0], y=xy_matrix[:, 1], z=xy_matrix[:, 2],
                               label=labels, tooltip=tooltips))



        #group by cluster
        groups = df.groupby('label')
        
        py.offline.iplot({
            'data': [
                Scatter3d(x=group.x,
                        y=group.y,
                        z=group.z,
                        # text=[ str[0:100] for str in group.title],
                        text=group.tooltip,
                        mode='markers',
                        marker=Marker(color=cc[name],size = name * 2)) for name, group in groups
            ],
            'layout': Layout(hovermode='closest')
        }, show_link=False, filename='123')
        
    else:
        print(len(tooltips))
        print(len(labels))
        print(len(set(labels)))
        
        
