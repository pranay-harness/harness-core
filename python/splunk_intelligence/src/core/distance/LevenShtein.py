# Copyright 2017 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import numpy as np

'''
Implements the LevenStein and Hirshberg edit distance
between 2 strings. There is special handling of the 
char 'x'.

Distances:

Cost(Add)   : x => no cost, other 10
Cost(Del)   : x => no cost, other 10
Cost(Match) : use distance_matrix 

'''


class LevenShteinDistance(object):
    def __init__(self, dist_func):
        self.dist_func = dist_func

    def find_optimal_alignment(self, a, b):
        result = ''
        result_indices = []
        edits = []
        row1 = np.array(np.zeros(len(a) + 1))
        for i in range(1, len(row1)):
            if a[i - 1] == 'x':
                row1[i] = row1[i - 1]
            else:
                row1[i] += row1[i - 1] + 10

        for i in range(1, len(b) + 1):
            row2 = np.array(np.zeros(len(a) + 1))
            row2[0] = row1[0] + 10 if b[i - 1] != 'x' else row1[0]
            edit = [0] * len(a)
            for j in range(1, len(row2)):
                vals = [row1[j - 1] + (self.dist_func(j-1, i-1, a[j - 1], b[i - 1])),
                        row1[j] + (10 if b[i - 1] != 'x' else 0),
                        row2[j - 1] + (10 if a[j - 1] != 'x' else 0)
                        ]
                row2[j] = np.min(vals)
                edit[j - 1] = np.argmin(vals)
            row1 = row2

            edits.append(edit)
        j = len(a) - 1
        i = len(b) - 1
        while i > -1 and j > -1:
            if edits[i][j] == 0:
                result += b[i]
                result_indices.append(i)
                i -= 1
                j -= 1
            elif edits[i][j] == 1:
                i -= 1
            else:
                result += 'x'
                j -= 1
                result_indices.append(-1)
        while j > -1:
            if edits[0][j] == 0:
                result += 'x'
                j -= 1
                result_indices.append(-1)
        return result[::-1], list(reversed(result_indices))


class Hirschberg(object):
    def __init__(self, dist_func):
        self.dist_func = dist_func
        self.ld = LevenShteinDistance(self.dist_func)

    def find_dist_row(self, a, b):
        row1 = np.array(np.zeros(len(a) + 1))
        for i in range(1, len(row1)):
            if a[i - 1] == 'x':
                row1[i] = row1[i - 1]
            else:
                row1[i] += row1[i - 1] + 10

        for i in range(1, len(b) + 1):
            row2 = np.array(np.zeros(len(a) + 1))
            row2[0] = row1[0] + 10 if b[i - 1] != 'x' else row1[0]
            for j in range(1, len(row2)):
                vals = [row1[j - 1] + self.dist_func(j-1, i-1, a[j - 1], b[i - 1]),
                        row1[j] + (10 if b[i - 1] != 'x' else 0),
                        row2[j - 1] + (10 if a[j - 1] != 'x' else 0)
                        ]
                row2[j] = np.min(vals)
            row1 = row2
        return row1

    def find_optimal_alignment(self, a, b):
        Z = ""
        indices = []
        if len(a) == 0:
            for i in range(len(b)):
                Z = Z + b[i]
                indices.append()
        elif len(b) == 0:
            for i in range(len(a)):
                Z = Z + 'x'
        elif len(b) == 1 or len(a) == 1:
            Z, I = self.ld.find_optimal_alignment(a, b)
        else:
            amid = len(a) / 2
            ScoreL = self.find_dist_row(b, a[:amid])
            ScoreR = self.find_dist_row(b[::-1], a[amid:][::-1])
            bmid = len(ScoreL) - np.argmin((ScoreL + ScoreR[::-1])[::-1]) - 1
            # If all values are the same
            # pick the first. Otherwise
            # we will loop indefinitely
            if bmid == len(ScoreL) - 1:
                bmid = 1
            Z = self.find_optimal_alignment(a[:amid], b[:bmid]) + \
                self.find_optimal_alignment(a[amid:], b[bmid:])
        return Z
