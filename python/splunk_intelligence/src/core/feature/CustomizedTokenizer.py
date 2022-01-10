# Copyright 2018 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import re

REGEXSUB = [
    (re.compile(r'([=])'), r' \1'),
    (re.compile(r'[.]{2,}'), r' ... '), # match any number of dots
    (re.compile(r'[?!]'), r' \g<0> '),
    (re.compile(r"([^'])' "), r"\1 ' "),
    (re.compile(r'[-]{2,}'), r' -- '), # match any number of dots
    (re.compile(r'([^\.])(\.)([\]\)}>"\']*)\s*$'), r'\1 \2\3 '),
    (re.compile(r'[\]\[\(\)\{\}\<\>]'), r' \g<0> '),  # handling parenthese
    (re.compile(r'"'), " '' "),  # ending
    (re.compile(r'(\S)(\'\')'), r'\1 \2 '),  # ending
    (re.compile(r"([^' ])('[sS]|'[mM]|'[dD]|') "), r"\1 \2 "),  # ending
    (re.compile(r"([^' ])('ll|'LL|'re|'RE|'ve|'VE|n't|N'T) "), r"\1 \2 ")]
STOPWORDS = set(['a', 'an', 'at', 'in', 'if', 'the','by', 'when', 'too', 'is', 'are', 'was', 'were', 'that', 'have', 'has',
                 'or', 'over', 'but', 'for', 'out', 'not', 'java', 'com', 'org', 'jar'])


def word_tokenize(sent):
    for regexp, substitution in REGEXSUB:
        sent = regexp.sub(substitution, sent)

    return sent.split()


class CustomizedTokenizer(object):

    @staticmethod
    def tokenizer(text):
        text = text.replace("'", '')
        text = text.replace('"', '')
        text = text.replace('``', '')
        # make sure there is not any space before or after : or =
        text = re.sub(r'(\s+:)', ':', text)
        text = re.sub(r'(:\s+)', ':', text)
        text = re.sub(r'(\s+=)', '=', text)
        text = re.sub(r'(=\s+)', '=', text)
        first_tokens = [word.lower() for phrase in text.split(' ') for word in phrase.split(',')]
        # remove the whole url tokens
        no_url_token = []
        for token in first_tokens:
            if not re.search('/', token) and not re.search('@', token):
                no_url_token.append(token)
        tokens = [word for word in word_tokenize(' '.join(no_url_token))]
        filtered_tokens = []
        for ugly_token in tokens:
            ugly_token.strip()
            # text that has : bring right side out the left side becuase it starts
            # with : will be filtered in the next step
            if re.search('([:])', ugly_token) and re.search('([a-zA-Z])', ugly_token):
                ugly_token = re.sub(r'([:])', r' \1', ugly_token)
            for token in ugly_token.split(' '):
                if len(re.findall('\.', token)) < 2 or re.search('[^0-9.]', token): # removing ip address but keep float
                    sub = str(len(filtered_tokens)) + 'xx'  # if replace with a fixed letter, it gets confused while training by seeing one word in different context
                    if not re.search('[^0-9.]', token):  # only number - float and int - no char
                        token = sub
                    else:
                        token = re.sub("[-+]?\d+", sub, token)  # all numbers +/- and float
                    for t in token.split('.'):
                        if not re.search('[^0-9a-zA-Z._-]', t) and re.search('[0-9a-zA-Z]', t) and t not in STOPWORDS and len(t)>=2:
                            filtered_tokens.append(t)

        if len(filtered_tokens) == 0:
            filtered_tokens.append(text)
        return filtered_tokens
