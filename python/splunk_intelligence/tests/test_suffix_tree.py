# Copyright 2017 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

from core.data_structures.SuffixTree import SuffixTree


def test_suffix_1():
    st = SuffixTree()
    st.build_naive("banannas")
    assert st.get_counts("ban") == 1
    assert st.get_counts("an") == 2
    assert st.get_counts("bna") == 0
    print(st.get_counts("ann"))
    assert st.get_counts("ann") == 1
    assert st.get_counts("as") == 1
    assert st.get_counts("banannas") == 1
    assert st.get_counts("aan") == 0


def test_suffix_2():
    st = SuffixTree()
    st.build_naive('abcdbcdbc')
    assert st.get_counts('bcd') == 2
    assert st.get_counts('b') == 3
    assert st.get_counts('cdb') == 2
    assert st.get_counts('a') == 1
    assert st.get_counts('bcb') == 0
