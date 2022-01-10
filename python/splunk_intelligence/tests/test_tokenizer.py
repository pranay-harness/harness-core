# Copyright 2017 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

from core.feature.Tokenizer import Tokenizer


def test_default_tokenizer():
    str = '''INFO  [2017-05-31 21:35:18,180] org.quartz.plugins.history.LoggingTriggerHistoryPlugin: Trigger DEFAULT.vJ3t7B6hTYyugnJ0JifqXA completed firing job vJ3t7B6hTYyugnJ0JifqXA.ARTIFACT_STREAM_CRON_GROUP at  21:35:18 05/31/2017 with resulting trigger instruction code: DO NOTHING'''
    tokens = Tokenizer.default_tokenizer(str)
    expected_tokens = ['info', 'org.quartz.plugins.history.loggingtriggerhistoryplugin', 'trigger', 'default.vj3t7b6htyyugnj0jifqxa', 'completed', 'firing', 'vj3t7b6htyyugnj0jifqxa.artifact_stream_cron_group', 'with', 'resulting', 'trigger', 'instruction', 'code', 'nothing']
    assert len(expected_tokens) == len(tokens)
    for expected_token in expected_tokens:
        assert expected_token in tokens


def test_default_tokenizer_single_token():
    str = '''172.31.15.70 - - [31/May/2017:21:35:12 +0000] \" GET /todolist/health-check HTTP/1.1\" 200 50'''
    tokens = Tokenizer.default_tokenizer(str)
    assert len(tokens) == 1
    assert tokens[0] == str
