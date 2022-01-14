from sources.FileLoader import FileLoader


def test_load_file():
    events = FileLoader.load_data('resources/wings15.json')
    assert len(events) == 457

    for event in events:
        assert '_time' in event
        assert '_raw' in event
        assert 'cluster_label' in event
        assert 'cluster_count' in event
