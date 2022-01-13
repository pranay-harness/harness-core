# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import json
import requests
import sys
import time
from core.util.lelogging import get_log
from datetime import datetime, timedelta

logger = get_log(__name__)


class NewRelicSource(object):
    def __init__(self, appId):
        self.appId = appId
        self.url = 'https://api.newrelic.com'

    def get_request(self, url, headers, max_retries=1):
        sleep_time = 1
        num_max_retries = max_retries
        while max_retries > 0:
            r = requests.get(url, headers=headers, verify=False, timeout=30)
            if r.status_code != 500 and r.status_code != 503:
                return json.loads(r.text), r.status_code
            else:
                max_retries = max_retries - 1
                if max_retries > 0:
                    time.sleep(sleep_time)
                    sleep_time = sleep_time * 2

        raise Exception(str(num_max_retries) + ' retries attempted, but unable to get request from ' + url)

    def get_node_instances(self):
        url = self.url + '/v2/applications/' + str(self.appId) + '/instances.json'
        headers = {"Accept": "application/json", "Content-Type": "application/json",
                   "X-Api-Key": '5ed76b50ebcfda54b77cd1daaabe635bd7f2e13dc6c5b11'}
        data, ret_code = data, ret_code = self.get_request(url, headers)
        return data['application_instances']

    def get_metric_info(self):
        url = self.url + '/v2/applications/' + str(self.appId) + '/metrics.json?name = WebTransaction/'
        headers = {"Accept": "application/json", "Content-Type": "application/json",
                   "X-Api-Key": '5ed76b50ebcfda54b77cd1daaabe635bd7f2e13dc6c5b11'}
        data, ret_code = self.get_request(url, headers)
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
                        data, ret_code = self.get_request(url, headers)
                        for metric in data['metric_data']['metrics']:
                            for cdIndex, timeslice in enumerate(metric['timeslices']):
                                if 'average_response_time' in timeslice['values']:
                                    result.append(
                                        dict(name=metric['name'], host=node['host'], dataCollectionMinute=cdIndex,
                                             values= dict(throughput=timeslice['values']['requests_per_minute']
                                             if 'requests_per_minute' in timeslice['values'] else -1,
                                             averageResponseTime=timeslice['values']['average_response_time'],
                                             apdexScore=-1,
                                             tag='default',
                                             error=-1,
                                             callCount=timeslice['values']['call_count'],
                                             requestsPerMinute=timeslice['values']['requests_per_minute'])))
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
