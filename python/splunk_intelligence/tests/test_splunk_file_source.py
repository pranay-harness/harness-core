from sources.SplunkFileSource import SplunkFileSource


def test_load_file():
    events = SplunkFileSource.load_data('tests/resources/wings15.json')
    assert len(events) == 457

    for event in events:
        assert '_time' in event
        assert '_raw' in event
        assert 'cluster_label' in event
        assert 'cluster_count' in event
