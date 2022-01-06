# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import threading
import time
from BaseHTTPServer import HTTPServer, BaseHTTPRequestHandler
from sources.HarnessLoader import HarnessLoader


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
    service_secret = '22ef5a98920448e7d3c70d9fc9566085'
    version_file_path = '../service_version.properties'

    server = HTTPServer(('', 18080), PostHandler)
    thread = threading.Thread(target=server.serve_forever)
    thread.daemon = True
    try:
        thread.start()
        print('test post timeout')
        HarnessLoader.send_request('http://localhost:18080', 'Dummy', version_file_path, service_secret)
        print('test get timeout')
        headers = {"Accept": "application/json", "Content-Type": "application/json"}
        HarnessLoader.get_request('http://localhost:18080', version_file_path, service_secret)
        print('finish')
    except:
        server.shutdown()
        raise
