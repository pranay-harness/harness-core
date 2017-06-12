import re

import nltk

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
        # filter out any tokens the contain punctuation other than dot(.)
        # filter out any tokens less that 4 characters
        for token in tokens:
            if len(token) > 3 and not re.search('[^0-9a-zA-Z.]', token) \
                    and re.search('[a-zA-Z]', token) and token:
                filtered_tokens.append(token)
        if len(filtered_tokens) == 0:
            filtered_tokens.append(text)
        return filtered_tokens
