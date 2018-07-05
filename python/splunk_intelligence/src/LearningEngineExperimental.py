import threading
import sys
import time
import argparse
import signal
import TimeSeriesML
from SplunkIntelOptimized import SplunkIntelOptimized
import ClusterInput
import os
import json

from core.util.lelogging import get_log
from sources.LogCorpus import LogCorpus
from sources.HarnessLoader import HarnessLoader


logger = get_log(__name__)
VERSIONFILEPATH = 'service_version.properties'
EXPNAME = 'doc2vec'



class Struct:
    def __init__(self, **entries):
        self.__dict__.update(entries)


def complete_url(server_url, url):
    print ('server url is ', server_url)
    if url:
        return server_url + url
def output_to_dict(corpus, options):

    return dict(query=options.query, application_id=options.application_id,
                state_execution_id=options.state_execution_id,
                log_collection_minute=options.log_collection_minute, control_events=corpus.control_events,
                test_events=corpus.test_events, unknown_events=corpus.anomalies,
                control_clusters=corpus.control_clusters, test_clusters=corpus.test_clusters,
                unknown_clusters=corpus.anom_clusters, cluster_scores=corpus.cluster_scores, score=corpus.score)

def run_learning_engine(parameters): #

    while 1:

        learning_api_url = complete_url(parameters.server_url, '/api/learning/get-next-exp-task?experimentName='+EXPNAME)
        try:
            text, status_code = HarnessLoader.get_request(learning_api_url, VERSIONFILEPATH, parameters.service_secret)
            version = HarnessLoader.get_accept_header(VERSIONFILEPATH)
            logger.info(str(status_code))
            logger.info('url is ' + str(learning_api_url) + ' and header is ' + str(version))
            if (status_code == 200) and text.get('resource'):
                # get task
                logger.info('A new task is pulled')
                options_dict = text.get('resource')
                if not options_dict.get('max_nodes_threshold'):
                    options_dict['max_nodes_threshold'] = 19
                options_dict['service_secret'] = parameters.service_secret
                options_dict['version_file_path'] = VERSIONFILEPATH
                options_dict['control_input_url'] = complete_url(parameters.server_url, options_dict['control_input_url'])
                options_dict['test_input_url'] = complete_url(parameters.server_url, options_dict['test_input_url'])
                options_dict['analysis_save_url'] = complete_url(parameters.server_url, options_dict['analysis_save_url'])
                options_dict['metric_template_url'] = complete_url(parameters.server_url,
                                                                 options_dict['metric_template_url'])
                if options_dict.get('feedback_url'):
                    options_dict['feedback_url'] = complete_url(parameters.server_url,
                                                                options_dict['feedback_url'])

                if options_dict['ml_analysis_type'] != 'TIME_SERIES':
                    options_dict['application_id'] = options_dict['appId']
                    options_dict['log_collection_minute'] = options_dict['analysis_minute']
                    del options_dict['analysis_minute']
                    options_dict['debug'] = False
                    options_dict['query'] = ' '.join(options_dict['query'])
                    options_dict['log_analysis_get_url'] = complete_url(parameters.server_url, options_dict['log_analysis_get_url'])
                    options_dict['log_analysis_save_url'] = options_dict['analysis_save_url']
                experimental_prev_state = {}
                # process the task
                logger.info('Executing the task')
                if options_dict['ml_analysis_type'] == 'LOG_ML':
                    a_result = HarnessLoader.load_prev_output_from_harness(options_dict['log_analysis_get_url'],
                                                                           options_dict['application_id'],
                                                                           options_dict['state_execution_id'],
                                                                           options_dict['query'],
                                                                           options_dict['log_collection_minute'],
                                                                           options_dict['version_file_path'],
                                                                           options_dict['service_secret'])

                    options_dict['experiment_name'] = EXPNAME
                    last_min = options_dict['log_collection_minute']
                    for min in range(last_min+1):
                        options_dict['log_collection_minute'] = min
                        options = Struct(**options_dict)
                        try:
                            corpus = LogCorpus()
                            corpus.load_from_harness(options, experimental_prev_state)
                            splunk_intel = SplunkIntelOptimized(corpus, options)
                            result = splunk_intel.run()
                            if len(result.control_events) != 0:
                                experimental_prev_state = output_to_dict(result, options)
                            del corpus
                        except Exception as e:
                            payload = dict(applicationId=options.application_id,
                                           workflowId=options.workflow_id,
                                           workflowExecutionId=options.workflow_execution_id,
                                           stateExecutionId=options.state_execution_id,
                                           serviceId=options.service_id,
                                           analysisMinute=options.log_collection_minute)
                            raise Exception(str(e) + ' for ' + json.dumps(payload))
                    b_result = experimental_prev_state
                    a_anomalies = a_result.get('unknown_clusters') if a_result.get('unknown_clusters') else []
                    b_anomalies = b_result.get('unknown_clusters') if b_result.get('unknown_clusters') else []

                    if len(a_anomalies) != len(b_anomalies):
                        result.save_to_harness(options.log_analysis_save_url,
                                               result.get_output_as_json(options), options.version_file_path,
                                               options.service_secret)
                    else:
                        result.save_to_harness(options.log_analysis_save_url,
                                               {}, options.version_file_path,
                                               options.service_secret)



                elif options_dict['ml_analysis_type'] == 'TIME_SERIES':
                    options = Struct(**options_dict)
                    pass
                elif options_dict['ml_analysis_type'] == 'LOG_CLUSTER':
                    options.input_url = options_dict['control_input_url']
                    options.output_url = options.analysis_save_url
                    options = Struct(**options_dict)
                    pass

                else:
                    logger.error(options_dict['ml_analysis_type'], ' analysis method is not defined ')
            else:

                # if there is not a task it waits 10s and re-tries again
                logger.info('Waiting for a new task')
                time.sleep(5)
        except Exception as e:
            logger.exception(e)
            time.sleep(10)


def parse(cli_args):
    parser = argparse.ArgumentParser()
    parser.add_argument("--https_port", type=int, required=True)
    parser.add_argument("--server_url", type=str, required=True)
    parser.add_argument("--service_secret", type=str, required=True)
    return parser.parse_args(cli_args)


def main(args):
    parameters = parse(args[1:])
    print (parameters)
    # server_url =
    # secret_key = '22ef5a98920448e7d3c70d9fc9566085' #os.environ.get('secret_key')
    run_learning_engine(parameters)


if __name__ == "__main__":
    main(sys.argv)


## python LearningEngine.py --https_port 18080  --server_url 'https://127.0.0.1:9090' --service_secret '22ef5a98920448e7d3c70d9fc9566085'
