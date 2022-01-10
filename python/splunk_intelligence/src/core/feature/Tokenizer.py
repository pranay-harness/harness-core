# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import nltk
import re

nltk.download('punkt')

"""
Tokenize text to build feature vectors. Suports tokenizing based on NLTK.
"""
class Tokenizer(object):


    @staticmethod
    def tokenize_line_only(text):
        """
        Split each line into a token

        :param text: input
        :return: tokens extracted
        """
        return [s.strip() for s in text.splitlines()]


    @staticmethod
    def default_tokenizer(text):
        """
        Uses nltk sentence and word tokenizer
        Drops tokens less of size 3 or lower
        Keeps alpha numeric with dot, and throws the rest

        :param text: input
        :return: tokens extracted
        """
        # first tokenize by sentence, then by word to ensure that punctuation is caught as it's own token
        tokens = [word.lower() for sent in nltk.sent_tokenize(text) for word in nltk.word_tokenize(sent)]
        filtered_tokens = []
        # filter out any tokens not containing letters (e.g., numeric tokens, raw punctuation)
        # filter out any tokens the contain punctuation other than dot(.), underscore(_), equals(=)
        # filter out any tokens less that 4 characters
        for token in tokens:
            if len(token) > 3 and not re.search('[^0-9a-zA-Z._=]', token) \
                    and re.search('[a-zA-Z]', token) and token:
                filtered_tokens.append(token)
        if len(filtered_tokens) == 0:
            filtered_tokens.append(text)

        return filtered_tokens


#tokens1 = Tokenizer.default_tokenizer(''' 2017-11-06 18:28:29,258 \u001b[32m[pool-10-thread-20]\u001b[0;39m \u001b[31mWARN \u001b[0;39m \u001b[36msoftware.wings.service.impl.AlertServiceImpl\u001b[0;39m - Alert opened: Alert(accountId=jQWSOdz5RX686BJa1w3ueA, type=NoActiveDelegates, status=Open, title=No delegates are available, category=Setup, severity=Error, alertData=NoActiveDelegatesAlert(accountId=jQWSOdz5RX686BJa1w3ueA), closedAt=0)  ''')
#print(tokens1)
#tokens2 = Tokenizer.default_tokenizer(''' 2017-11-06 18:28:29,817 \u001b[32m[pool-10-thread-2]\u001b[0;39m \u001b[31mWARN \u001b[0;39m \u001b[36msoftware.wings.service.impl.AlertServiceImpl\u001b[0;39m - Alert opened: Alert(accountId=AYbXJY4nTleLujUGO-0QGg, type=NoActiveDelegates, status=Open, title=No delegates are available, category=Setup, severity=Error, alertData=NoActiveDelegatesAlert(accountId=AYbXJY4nTleLujUGO-0QGg), closedAt=0) ''')
#print(tokens2)
#
# print(len(set(tokens1).intersection(set(tokens2))))
# print(len(set(tokens1).union(set(tokens2))))
#
#
#intersection = set(tokens1).intersection(set(tokens2))
#union = set(tokens1).union(set(tokens2))
#print (float(len(intersection)) / len(union))
