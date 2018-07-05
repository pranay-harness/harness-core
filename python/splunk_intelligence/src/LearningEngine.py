import argparse
import sys
import os
import subprocess
import threading
import time
import json
from BaseHTTPServer import HTTPServer, BaseHTTPRequestHandler
import ClusterInput
import SplunkIntelOptimized
import TimeSeriesML
from core.util.lelogging import get_log
from sources.HarnessLoader import HarnessLoader

logger = get_log(__name__)
process = False
VERSIONFILEPATH = 'service_version.properties'


class PostHandler(BaseHTTPRequestHandler):

    def do_POST(self):
        self.send_response(200)
        command = self.path
        global process
        if 'start_processing' in command:
            # TODO: if the process dies and is restarted it assigns the previous process value
            process = True
        elif 'stop_processing' in command:
            process = False
        else:
            self.send_response(501)
            logger.info('Not a valid URI')
        return

class Struct:
    def __init__(self, **entries):
        self.__dict__.update(entries)
# class Killer:
#     kill_now = False
#
#     def __init__(self):
#         signal.signal(signal.SIGINT, self.signal_handler)
#         signal.signal(signal.SIGTERM, self.signal_handler)
#
#     def signal_handler(self, signum, frame):
#
#         self.kill_now = True

def complete_url(server_url, url):
    print ('server url is ', server_url)
    if url:
        return server_url + url


def run_learning_engine(parameters): #
    # killer = Killer()
    server = HTTPServer(('', parameters.https_port), PostHandler)
    thread = threading.Thread(target=server.serve_forever)
    thread.daemon = True
    thread.start()
    while 1:
        if 1: #process:
            learning_api_url = complete_url(parameters.server_url, '/api/learning/get-next-task')
            try:
                text, status_code = HarnessLoader.get_request(learning_api_url, VERSIONFILEPATH, parameters.service_secret)
                version = HarnessLoader.get_accept_header(VERSIONFILEPATH)
                logger.info(str(status_code))
                logger.info('url is ' + str(learning_api_url) + ' and header is ' + str(version))
                if (status_code == 200) and text.get('resource'):
                    # get task
                    options_dict = text.get('resource')
                    logger.info('A new task is pulled for ' + json.dumps(options_dict))
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
                        options_dict['debug'] = False
                        options_dict['query'] = ' '.join(options_dict['query'])
                        options_dict['log_analysis_get_url'] = complete_url(parameters.server_url, options_dict['log_analysis_get_url'])
                        options_dict['log_analysis_save_url'] = options_dict['analysis_save_url']

                    options = Struct(**options_dict)
                    # process the task
                    logger.info('Executing the task')
                    if options_dict['ml_analysis_type'] == 'LOG_ML':
                        if str(options_dict.get('feature_name')).lower() =='neural_net' and str(os.environ.get('learning_env')).lower()!='on_prem':
                            options_dict['sim_threshold'] = 0.96
                            options = json.dumps(options_dict)
                            if os.environ.get('learning_env'):
                                child_file_name = 'LogNeuralNet.pyc'
                            else:
                                child_file_name = 'LogNeuralNet.py'
                            logger.info('Starting neural nets analysis')
                            dtv_child = subprocess.Popen(['python', child_file_name, options])
                            dtv_child.wait()
                        else:
                            SplunkIntelOptimized.main(options)
                    elif options_dict['ml_analysis_type'] == 'TIME_SERIES':
                        TimeSeriesML.main(options)
                    elif options_dict['ml_analysis_type'] == 'LOG_CLUSTER':
                        options.input_url = options_dict['control_input_url']
                        options.output_url = options.analysis_save_url
                        options.nodes = options.control_nodes
                        ClusterInput.main(options)
                    else:
                        logger.error(options_dict['ml_analysis_type'], ' analysis method is not defined ')
                else:

                    # if there is not a task it waits 10s and re-tries again
                    logger.info('Waiting for a new task')
                    time.sleep(5)
            except Exception as e:
                logger.exception(e)
                time.sleep(10)
        else:
            ## if process has not started, it sleeps then wakes up and checks if process is started
            time.sleep(1)
        # if killer.kill_now:
        #     print('Process is killed')
        #     sys.exit(5)


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


## python LearningEngine.py --https_port 18080  --server_url 'https://127.0.0.1:9090' --service_secret '645bfccbab762e12e74645d04bf19c61'
