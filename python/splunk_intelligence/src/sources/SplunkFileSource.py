import json
import logging

""" 
Read splunk dumps from file 
"""


class SplunkFileSource(object):
    # load data from json
    @staticmethod
    def load_data(data_source_name):
        logging.info('loading file ' + data_source_name)
        with open(data_source_name, 'r') as read_file:
            return json.loads(read_file.read())

    @staticmethod
    def load_prod_data(data_source_name):
        logging.info('loading file ' + data_source_name)
        result = []
        with open(data_source_name, 'r') as read_file:
            content = read_file.readlines()
        for x in content:
            result.append(json.loads(x.strip()))

        return result
