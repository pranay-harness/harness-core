import argparse
import sys
import requests


def parseArgs(cli_args):
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", required=True)
    return parser.parse_args(cli_args)


def main(args):
    # run_debug()

    # create options
    print(args)
    options = parseArgs(args[1:])
    while True:
        r = requests.get(options.url, verify=False)
        print(r.status_code)



if __name__ == "__main__":
    main(sys.argv)