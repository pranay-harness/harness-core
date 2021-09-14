# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0

import argparse
import requests
import sys
import time


def parse_args(cli_args):
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", required=True)
    parser.add_argument("--run_time", required=True)
    return parser.parse_args(cli_args)


def main(args):
    # create options
    print(args)
    options = parse_args(args[1:])
    timeout = time.time() + 60 * int(options.run_time)
    while True:
        if time.time() > timeout:
            break
        r = requests.get(options.url, verify=False, timeout=10)
        # print(r.status_code)
        time.sleep(10 / 1000.0)

if __name__ == "__main__":
    main(sys.argv)
