# Copyright 2017 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import json
import logging

""" 
Read splunk dumps from file 
"""


class FileLoader(object):
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
