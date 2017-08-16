import json
import logging
import sys
import time

import requests

logger = logging.getLogger(__name__)


class SplunkHarnessLoader(object):
    @staticmethod
    def get_request(url, max_retries=1):
        sleep_time = 1
        try:
            while max_retries > 0:
                r = requests.get(url, verify=False)
                if r.status_code != 500 and r.status_code != 503:
                    return json.loads(r.text), r.status_code
                else:
                    max_retries = max_retries - 1
                    if max_retries > 0:
                        time.sleep(sleep_time)
                        sleep_time = sleep_time * 2
        except Exception as e:
            logging.error(e)

        logger.error("Unable to connect to " + url)
        sys.exit(1)

    @staticmethod
    def send_request(url, payload, headers, ssl_verify=False, read_timeout=10, max_retries=1):

        sleep_time = 1
        try:
            while max_retries > 0:
                r = requests.post(url, data=payload, headers=headers, verify=ssl_verify, timeout=read_timeout)
                if r.status_code != 500 and r.status_code != 503:
                    return r.text, r.status_code
                else:
                    max_retries = max_retries - 1
                    if max_retries > 0:
                        logger.info('Retrying ' + url + 'for ' + json.dumps(payload) + ' in ' + sleep_time + 'secs')
                        time.sleep(sleep_time)
                        sleep_time = sleep_time * 2
        except Exception as e:
            logging.error(e)

        logger.error("Unable to connect to " + url)
        sys.exit(1)

    @staticmethod
    def post_to_wings_server(url, auth_token, response):
        headers = {"Accept": "application/json", "Content-Type": "application/json", "Authorization": "ExternalService " + auth_token}
        text, status_code = SplunkHarnessLoader.send_request(url, response, headers, False, 3)
        logger.info("Posting results to " + url)
        if status_code != 200:
            logger.error("Failed to post to Harness manager at " + url + " . Got status " + str(status_code))
            logger.error("Got back text " + text)
            sys.exit(-1)

    @staticmethod
    def load_prev_output_from_harness(url, auth_token, app_id, state_execution_id, query):
        headers = {"Accept": "application/json", "Content-Type": "application/json", "Authorization": "ExternalService " + auth_token}
        payload = dict(applicationId=app_id, stateExecutionId=state_execution_id, query=query)
        logger.info('Fetching data from Harness Manager for ' + json.dumps(payload))
        text, status_code = SplunkHarnessLoader.send_request(url, json.dumps(payload), headers, False, 3)
        if status_code != 200:
            logger.error(
                "Failed to fetch data from Harness manager. Got status_code = " + str(
                    status_code) + ' for ' + json.dumps(
                    payload))
            sys.exit(-1)
        return json.loads(text)['resource']

    @staticmethod
    def load_from_harness_raw(url, auth_token, app_id, workflow_id, state_execution_id, service_id, log_collection_minute, nodes, query):
        headers = {"Accept": "application/json", "Content-Type": "application/json",
                   "Authorization": "ExternalService " + auth_token}
        payload = dict(applicationId=app_id, workflowId=workflow_id, stateExecutionId=state_execution_id,
                       serviceId=service_id, logCollectionMinute=log_collection_minute, nodes=nodes,
                       query=query)
        logger.info('Fetching data from Harness Manager for ' + json.dumps(payload))
        text, status_code = SplunkHarnessLoader.send_request(url, json.dumps(payload), headers, False, 3)
        if status_code != 200:
            logger.error(
                "Failed to fetch data from Harness manager. Got status_code = " + str(
                    status_code) + ' for ' + json.dumps(
                    payload))
            sys.exit(-1)
        data = json.loads(text)
        if data is None or data['resource'] is None:
            logging.error("Server returned no data for " + json.dumps(payload))
            sys.exit(1)

        return data

    #TODO rename wings to harness
    @staticmethod
    def load_from_wings_server(url, auth_token, app_id, workflow_id, state_execution_id, service_id, log_collection_minute, nodes, query):
        data = SplunkHarnessLoader.load_from_harness_raw(url, auth_token, app_id, workflow_id,
                                                     state_execution_id, service_id, log_collection_minute, nodes, query)
        raw_events = []
        for resp in data['resource']:
            raw_event = {'cluster_count': resp['count'], 'cluster_label': resp['clusterLabel'],
                         '_time': resp['timeStamp'], '_raw': resp['logMessage'], 'host': resp['host']}
            raw_events.append(raw_event)

        return raw_events

# print(json.dumps(SplunkHarnessLoader.load_from_wings_server(
#    'https://localhost:9090/api/splunk/get-logs?accountId=kmpySmUISimoRrJL6NL73w',
#    'm9XTWIcnS2OVk-ys0wiX-Q',
#    1497920520000,
#    1497945241000,
#    ["ip-172-31-11-228", "ip-172-31-1-93"])))
