# Copyright 2017 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

from sources.FileLoader import FileLoader


def test_load_file():
    events = FileLoader.load_data('resources/wings15.json')
    assert len(events) == 457

    for event in events:
        assert '_time' in event
        assert '_raw' in event
        assert 'cluster_label' in event
        assert 'cluster_count' in event
