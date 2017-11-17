import json

import requests
import sys

from sources.HarnessLoader import HarnessLoader
from datetime import datetime, timedelta
import logging
import time

format = "%(asctime)-15s %(levelname)s %(message)s"
logging.basicConfig(level=logging.INFO, format=format)
logger = logging.getLogger(__name__)


class NewRelicSource(object):
    def __init__(self, appId):
        self.appId = appId
        self.url = 'https://api.newrelic.com'

    def get_node_instances(self):
        url = self.url + '/v2/applications/' + str(self.appId) + '/instances.json'
        headers = {"Accept": "application/json", "Content-Type": "application/json",
                   "X-Api-Key": '5ed76b50ebcfda54b77cd1daaabe635bd7f2e13dc6c5b11'}
        data, ret_code = HarnessLoader.get_request(url, headers)
        return data['application_instances']

    def get_metric_info(self):
        url = self.url + '/v2/applications/' + str(self.appId) + '/metrics.json?name = WebTransaction/'
        headers = {"Accept": "application/json", "Content-Type": "application/json",
                   "X-Api-Key": '5ed76b50ebcfda54b77cd1daaabe635bd7f2e13dc6c5b11'}
        data, ret_code = HarnessLoader.get_request(url, headers)
        return data['metrics']

    def get_metric_data(self, control_hosts, test_hosts, from_time, to_time):
        test_data = self.fetch_data(from_time, to_time)
        control_data = self.fetch_data(from_time - timedelta(minutes=15), from_time)
        print(json.dumps(test_data))
        print(json.dumps(control_data))

    def get_metric_data(self):
        to_time = datetime.utcnow()
        from_time = to_time - timedelta(minutes=15)
        test_data = self.fetch_data(from_time, to_time)
        control_data = self.fetch_data(from_time - timedelta(minutes=15), from_time)
        print(json.dumps(test_data))
        print(json.dumps(control_data))

    def live_analysis(self, control_hosts, test_hosts, from_time, to_time):
        data = self.fetch_data(from_time, to_time)
        control_data = []
        test_data = []
        for d in data:
            if d['host'] in control_hosts:
                control_data.append(d)
            elif d['host'] in test_hosts:
                test_data.append(d)
        return control_data, test_data

    def fetch_data(self, from_time, to_time):
        headers = {"Accept": "application/json", "Content-Type": "application/json",
                   "X-Api-Key": '5ed76b50ebcfda54b77cd1daaabe635bd7f2e13dc6c5b11'}
        node_instances = self.get_node_instances()
        metric_info = self.get_metric_info()
        metric_string = ''
        count = 0
        result = []
        for info in metric_info:
            if 'WebTransaction' in info['name']:
                metric_string += "names[]=" + info['name'] + "&"
                count += 1
                if count == 15:
                    metric_string = metric_string[:-1]

                    for node in node_instances:
                        url = self.url + '/v2/applications/' + str(self.appId) + '/instances/' + str(node['id']) \
                              + '/metrics/data.json?' + metric_string + \
                              "&from=" + str(from_time) + "&to=" + str(to_time)
                        data, ret_code = HarnessLoader.get_request(url, headers)
                        for metric in data['metric_data']['metrics']:
                            for index, timeslice in enumerate(metric['timeslices']):
                                if 'average_response_time' in timeslice['values']:
                                    result.append(
                                        dict(name=metric['name'], host=node['host'], dataCollectionMinute=index,
                                             throughput=timeslice['values']['requests_per_minute']
                                             if 'requests_per_minute' in timeslice['values'] else -1,
                                             averageResponseTime=timeslice['values']['average_response_time'],
                                             apdexScore=-1,
                                             error=-1,
                                             callCount=timeslice['values']['call_count']))
                    count = 0
                    metric_string = ''
        return result


def main(args):
    source = NewRelicSource(56513566)
    to_time = datetime.utcnow()
    from_time = to_time - timedelta(minutes=15)
    control_data, test_data = source.live_analysis(set(['ip-172-31-58-253']), set(['ip-172-31-8-144', 'ip-172-31-12-79',
                                                                                   'ip-172-31-13-153',
                                                                                   'ip-172-31-1-92']), from_time,
                                                   to_time)
    print(json.dumps(test_data))
    print(json.dumps(control_data))


if __name__ == "__main__":
    main(sys.argv)
