import threading

import sys

import time
from sources.SplunkHarnessLoader import SplunkHarnessLoader
from BaseHTTPServer import HTTPServer, BaseHTTPRequestHandler


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

def test_timeout():
    server = HTTPServer(('', 18080), PostHandler)
    thread = threading.Thread(target=server.serve_forever)
    thread.daemon = True
    try:
        thread.start()
        print('test post timeout')
        SplunkHarnessLoader.send_request('http://localhost:18080', dict(message='Dummy'), headers=None)
        print('test get timeout')
        SplunkHarnessLoader.get_request('http://localhost:18080', 1)
        print('finish')
    except:
        server.shutdown()
        raise