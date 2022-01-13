# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import sys
import time

class SuffixTree(object):
    def __init__(self):
        self.root = _SNode()

    def get_node(self, is_terminal = False, start_index=-1):
        node = _SNode()
        node.is_terminal = is_terminal
        node.start_index = start_index
        return node

    def build_naive(self, s):
        s = s + '$'
        for i in range(0, len(s)):
            self.add_suffix_naive(self.root, i, s[i:])
        self.preprocess()


    def add_suffix_naive(self, curr_node, start_index, s):
        found = False
        for key, node in curr_node.edges.items():
            ind = self.common_prefix(s, key)
            if ind != 0:
                found = True
                if ind == len(key):
                    self.add_suffix_naive(node,  start_index, s[ind:])
                else:
                    curr_node.edges[s[:ind]] = self.get_node(False)
                    curr_node.edges[s[:ind]].edges[key[ind:]] = node
                    del curr_node.edges[key]
                    self.add_suffix_naive(curr_node.edges[s[:ind]], start_index, s[ind:])
                break
        if not found:
            curr_node.edges[s] = self.get_node(True, start_index)

    def preprocess(self):
        self.build_counts(self.root)

    def build_counts(self, node):
        if node.is_terminal:
            node.counts = 1
            return 1

        for key, child in node.edges.items():
            node.counts += self.build_counts(child)
        return node.counts

    '''
        Return the cdIndex of the first match.
        Treats the strings as array fdIndex from
        1. i.e, the cdIndex of the first element is 1.
        
        returns 0 if no common prefix.
    '''
    def common_prefix(self, a, b):
        i = -1
        for i, (x, y) in enumerate(zip(a, b)):
            if x != y:
                i -= 1
                break
        return i + 1

    def get_counts(self, s):
        node = self.root
        s1 = s
        while 1:
            ind = 0
            for key, child in node.edges.items():
                ind = self.common_prefix(s1, key)
                if ind > 0:
                    break
            if ind == 0:
                return 0
            if ind == len(s1):
                return child.counts
            else:
                node = child
                s1 = s1[ind:]




class _SNode():
    def __init__(self):
        self.is_terminal = False
        self.start_index = -1
        self.edges = {}
        self.counts = 0


def main(args):
    st = SuffixTree()
    start_time = time.time()
    st.build_naive('abcdbcdbc')
    print("--- %s seconds ---" % (time.time() - start_time))
    print(st.get_counts('bcd'))
    print('finish')

if __name__ == "__main__":
    main(sys.argv)
