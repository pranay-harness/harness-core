import json
import time
from datetime import timedelta, datetime
import newrelic.agent
import jwt
import ConfigParser
from StringIO import StringIO

import requests
from core.util.lelogging import get_log

logger = get_log(__name__)



class HarnessLoader(object):
    @staticmethod
    def get_accept_header(version_file_path):
        config = ConfigParser.RawConfigParser()
        # dummy header is added for configParser. It assumes the file has a header
        with open(version_file_path) as f:
            f = StringIO('[header]\n' + f.read())
        config.readfp(f)
        version = config.get('header', 'learning_engine')
        return 'application/' + version +'+json'

    @staticmethod
    def generate_token(service_secret):
        to_time = datetime.utcnow()
        exp_time = to_time + timedelta(minutes=60)
        encoded_jwt = jwt.encode({'iss': 'Harness Inc', 'exp': exp_time}, service_secret, algorithm='HS256')
        return encoded_jwt

    @staticmethod
    def make_header(version_file_path, service_secret):
        accept_header = HarnessLoader.get_accept_header(version_file_path)
        auth_token = HarnessLoader.generate_token(service_secret)

        headers = {"Accept": accept_header, "Content-Type": "application/json",
                   "Authorization": "LearningEngine " + auth_token}
        return headers


    @newrelic.agent.background_task()
    @staticmethod
    def get_request(url, version_file_path, service_secret, max_retries=1):
        sleep_time = 1
        headers = HarnessLoader.make_header(version_file_path, service_secret)
        while max_retries > 0:
            r = requests.get(url, headers=headers, verify=False, timeout=30)
            if r.status_code != 500 and r.status_code != 503:
                return json.loads(r.text), r.status_code
            else:
                max_retries = max_retries - 1
                if max_retries > 0:
                    time.sleep(sleep_time)
                    sleep_time = sleep_time * 2

        raise Exception(str(max_retries) + 'is tried, but unable to get request from' + url)




    @newrelic.agent.background_task()
    @staticmethod
    def send_request(url, payload, version_file_path, service_secret, ssl_verify=False, read_timeout=30, max_retries=1):

        sleep_time = 1

        headers = HarnessLoader.make_header(version_file_path, service_secret)

        while max_retries > 0:
            r = requests.post(url, data=payload, headers=headers, verify=ssl_verify, timeout=read_timeout)
            if r.status_code != 500 and r.status_code != 503:
                return r.text, r.status_code
            else:
                max_retries = max_retries - 1
                if max_retries > 0:
                    logger.info('Retrying ' + url + 'for ' + json.dumps(payload) + ' in ' + str(sleep_time) + 'secs')
                    time.sleep(sleep_time)
                    sleep_time = sleep_time * 2

        raise Exception(str(max_retries) + 'is tried, but unable to post request to ' + url)



    @staticmethod
    def post_to_wings_server(url, response, version_file_path, service_secret):
        text, status_code = HarnessLoader.send_request(url, response, version_file_path, service_secret,
                                                       ssl_verify=False, max_retries=3)
        logger.info("Posting results to " + url)
        if status_code != 200:
            raise Exception("Failed to post to Harness manager at " + url + " . Got status " + str(status_code)
                            + " Got back text " + text)




    @staticmethod
    def load_prev_output_from_harness(url, app_id, state_execution_id, query, log_collection_minute,
                                      version_file_path, service_secret):

        payload = dict(applicationId=app_id, stateExecutionId=state_execution_id, query=query,
                       logCollectionMinute=log_collection_minute)
        logger.info('Starting Fetching data from Harness Manager for ' + json.dumps(payload))
        text, status_code = HarnessLoader.send_request(url, json.dumps(payload), version_file_path, service_secret,
                                                       ssl_verify=False, max_retries=3
                                                       )
        if status_code != 200:
            raise Exception(
                "Failed to fetch data from Harness manager. Got status_code = " + str(
                    status_code) + ' for ' + json.dumps(
                    payload))
        return json.loads(text)['resource']

    @staticmethod
    def load_from_harness_raw(url, app_id, workflow_id, state_execution_id, service_id,
                              log_collection_minute, nodes, query, version_file_path, service_secret):

        payload = dict(applicationId=app_id, workflowId=workflow_id, stateExecutionId=state_execution_id,
                       serviceId=service_id, logCollectionMinute=log_collection_minute, nodes=nodes,
                       query=query)
        logger.info('Fetching data from Harness Manager for ' + json.dumps(payload))
        text, status_code = HarnessLoader.send_request(url, json.dumps(payload), version_file_path, service_secret,
                                                       ssl_verify=False, max_retries=3)
        if status_code != 200:
            raise Exception(
                "Failed to fetch data from Harness manager. Got status_code = " + str(
                    status_code) + ' for ' + json.dumps(
                    payload))
        data = json.loads(text)
        if data is None or data['resource'] is None:
            raise Exception("Server returned no data for " + json.dumps(payload))

        return data

    # TODO replace the load_from_harness_raw with this when working on Splunk
    @staticmethod
    def load_from_harness_raw_new(url, payload, version_file_path, service_secret):
        logger.info('Fetching data from Harness Manager for ' + json.dumps(payload))
        print(url)
        text, status_code = HarnessLoader.send_request(url, json.dumps(payload), version_file_path, service_secret,
                                                       ssl_verify=False,
                                                       max_retries=3)
        if status_code != 200:
            raise Exception(
                "Failed to fetch data from Harness manager. Got status_code = " + str(
                    status_code))
        data = json.loads(text)
        if data is None or data['resource'] is None:
            raise Exception("Server returned no data ")
        return data

    # TODO rename wings to harness
    @staticmethod
    def load_from_wings_server(url,app_id, workflow_id, state_execution_id, service_id,
                               log_collection_minute, nodes, query, version_file_path, service_secret):
        data = HarnessLoader.load_from_harness_raw(url,app_id, workflow_id,
                                                   state_execution_id, service_id, log_collection_minute, nodes, query,
                                                   version_file_path, service_secret)
        raw_events = []
        for resp in data['resource']:
            raw_event = {'cluster_count': resp['count'], 'cluster_label': resp['clusterLabel'],
                         '_time': resp['timeStamp'], '_raw': resp['logMessage'], 'host': resp['host']}
            raw_events.append(raw_event)

        return raw_events

