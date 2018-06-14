import argparse
import json
import time
import TimeSeriesML
import threading

from sources.FileLoader import FileLoader
from sources.HarnessLoader import HarnessLoader
from BaseHTTPServer import HTTPServer, BaseHTTPRequestHandler
from SplunkIntelOptimized import SplunkIntelOptimized
from sources.LogCorpus import LogCorpus


class PostHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        time.sleep(1)
        self.send_response(200)
        return

    def do_GET(self):
        time.sleep(1)
        self.send_response(200)
        self.end_headers()
        self.wfile.write('{}')
        return


def test_time_series_parallelize_processing():
    metric_template = FileLoader.load_data('tests/resources/ts/metric_template.json')
    parser = argparse.ArgumentParser()
    parser.add_argument("--analysis_minute", type=int, required=True)
    parser.add_argument("--analysis_start_minute", type=int, required=True)
    parser.add_argument("--tolerance", type=int, required=True)
    parser.add_argument("--smooth_window", type=int, required=True)
    parser.add_argument("--min_rpm", type=int, required=True)
    parser.add_argument("--comparison_unit_window", type=int, required=True)
    parser.add_argument("--parallel_processes", type=int, required=True)
    parser.add_argument('--max_nodes_threshold', nargs='?', const=19, type=int)
    options = parser.parse_args(['--analysis_minute', '30', '--analysis_start_minute', '0', '--tolerance', '1', '--smooth_window', '3',
                                 '--min_rpm', '10', '--comparison_unit_window', '3', '--parallel_processes', '2',
                                 '--max_nodes_threshold', '19'])

    with open("/Users/parnianzargham/Desktop/portal/python/splunk_intelligence/time_series/control_live.json",
              'r') as read_file:
        control_metrics = json.loads(read_file.read())
    with open("/Users/parnianzargham/Desktop/portal/python/splunk_intelligence/time_series/test_live.json",
              'r') as read_file:
        test_metrics = json.loads(read_file.read())
    # we need to call function with the module that it is inside it, otherwise it is not going to work
    result = TimeSeriesML.parallelize_processing(options, metric_template, control_metrics, test_metrics)


def test_HarnessLoader_post_get():
    server = HTTPServer(('', 18080), PostHandler)
    thread = threading.Thread(target=server.serve_forever)
    thread.daemon = True
    thread.start()
    HarnessLoader.send_request('http://localhost:18080', dict(message='Dummy'), headers=None)

    headers = {"Accept": "application/json", "Content-Type": "application/json"}
    HarnessLoader.get_request('http://localhost:18080', headers=headers)
    server.shutdown()


def test_SplunkIntelOptimized_run():

    data = FileLoader.load_data('tests/resources/logs/log_ml_out_1.json')
    control = data['control_events']
    test = data['test_events']
    corpus = LogCorpus()
    for events in control.values():
        for event in events:
            corpus.add_event(event, 'control_prev')

    for events in test.values():
        for event in events:
            corpus.add_event(event, 'test_prev')

    sio = SplunkIntelOptimized(corpus, SplunkIntelOptimized.parse(['--sim_threshold', '0.9'])).run()

    #assert len(result.anom_clusters) == 2



if __name__ == "__main__":
    while 1:
        #test_time_series_parallelize_processing()
        #test_SplunkIntelOptimized_run()

        test_HarnessLoader_post_get()
