import threading

import sys

import time
from sources.HarnessLoader import HarnessLoader
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
        HarnessLoader.send_request('http://localhost:18080', dict(message='Dummy'), headers=None)
        print('test get timeout')
        headers = {"Accept": "application/json", "Content-Type": "application/json"}
        HarnessLoader.get_request('http://localhost:18080', headers=headers)
        print('finish')
    except:
        server.shutdown()
        raise