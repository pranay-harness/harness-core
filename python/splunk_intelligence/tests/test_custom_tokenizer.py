from core.feature.CustomizedTokenizer import CustomizedTokenizer


def test_default_tokenizer():
    text1 = '''testing time 21:35:18, testing numbers 1221 1.23, testing acount id with dot ARTIFACT_STREAM_CRON_GROUP.2D0msU62QqSaA61gD_UvCg, testing more account id 91738921472hhdh  12212331332  12123-1313-13131-4, testing colon and equal, this = sasa , this is:1sadad1 
     testing urls shbshjfs/sfsfs/sf/fsf/$ge ,  testing ips 1212.12.122.221 testting "quote" '''
    tokens = CustomizedTokenizer.tokenizer(text1)
    expected_tokens = ['testing', 'time', 'testing', 'numbers', '4xx', '5xx', 'testing', 'acount', 'id', 'with', 'dot', 'artifact_stream_cron_group', '11xxd11xxmsu11xxqqsaa11xxgd_uvcg', 'testing', 'more', 'account', 'id', '17xxhhdh', '18xx', '19xx19xx19xx19xx', 'testing', 'colon', 'and', 'equal', 'this', 'this', 'testing', 'urls', 'testing', 'ips', 'testting', 'quote']
    assert len(expected_tokens) == len(tokens)
    for expected_token in expected_tokens:
        assert expected_token in tokens


