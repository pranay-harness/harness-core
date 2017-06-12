import json

""" 
Read splunk dumps from file 
"""
class SplunkFileSource(object):
    # load data from json
    @staticmethod
    def load_data(data_source_name):
        print('loading file ' + data_source_name)
        with open(data_source_name, 'r') as read_file:
            return json.loads(read_file.read())
