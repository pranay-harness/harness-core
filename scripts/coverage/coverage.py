import csv
import json
import os
import pprint
import pycurl
import sys
from io import BytesIO

pp = pprint.PrettyPrinter(indent=4)
token = os.environ['SONAR_TOKEN']
baseUrl = "http://sonar.harness.io/api/measures/component_tree?component=portal:<PATH>&metricKeys=ncloc,coverage,lines_to_cover,branch_coverage,branch_coverage,uncovered_lines,conditions_to_cover&qualifiers=<Q>"
regexUrl = "&q=<REGEX>"
metric_key = "baseComponent.measures.value"
child_metric_key = "components"


def is_file(path):
    return path.find(".java") != -1


def is_regex(path):
    return path.find("@") != -1


def is_folder(path):
    return is_file(path) == 0


def get_json_from_api(url):
    b_obj = BytesIO()
    curl = pycurl.Curl()
    curl.setopt(pycurl.URL, url)
    curl.setopt(pycurl.USERPWD, '%s:' % token)
    curl.setopt(curl.WRITEDATA, b_obj)
    curl.perform()
    return json.loads(b_obj.getvalue())


def find_item_with_key(data, key, value):
    for v in data:
        if v.get(key) == value:
            return v.get("value")


def find_cumulative_with_key(data, key, value):
    total = 0
    for v in data:
        val = extract_from_json(v.get("measures"), "metric", value);
        if val is not None:
            total = total + float(val)
    return total


def extract_from_json_child_components(data, key, metric):
    return find_cumulative_with_key(data.get(key), "metric", metric)


def extract_from_json(data, key, metric):
    if key.find(".") == -1:
        return find_item_with_key(data, "metric", metric)
    keys = key.split(".")
    return extract_from_json(data[keys[0]], ".".join(keys[1:]), metric)


def is_child_needed(filePath):
    return is_regex(filePath)


def prepare_url(fileFolder):
    file_url = baseUrl.replace("<PATH>", fileFolder.split("@")[0])
    if is_regex(fileFolder):
        file_url = file_url.replace("<Q>", "FIL")
        regex = fileFolder.split("@")[1]
        file_url = file_url + regexUrl.replace("<REGEX>", regex)
    elif is_folder(fileFolder):
        file_url = file_url.replace("<Q>", "DIR")
    elif is_file(fileFolder):
        file_url = file_url.replace("<Q>", "FIL")
    return file_url


def calculate_coverage(coverage_file_path):
    url = prepare_url(coverage_file_path)
    result = get_json_from_api(url)
    if is_child_needed(coverage_file_path):
        uncovered_lines = extract_from_json_child_components(result, child_metric_key, "uncovered_lines")
        lines_to_cover = extract_from_json_child_components(result, child_metric_key, "lines_to_cover")
        coverage = int((1 - uncovered_lines / lines_to_cover) * 100)
        branch_coverage = "NA"
        conditions_to_cover = "NA"
    else:
        coverage = extract_from_json(result, metric_key, "coverage")
        lines_to_cover = extract_from_json(result, metric_key, "lines_to_cover")
        branch_coverage = extract_from_json(result, metric_key, "branch_coverage")
        conditions_to_cover = extract_from_json(result, metric_key, "conditions_to_cover")

    comma_str = "{0} ,{1}, {2}, {3}%, {4}".format(coverage_file_path,
                                                  coverage, lines_to_cover, branch_coverage,
                                                  conditions_to_cover)
    print(comma_str)
    return comma_str


def export_file(data):
    with open("out.csv", 'w') as csvfile:
        csv_writer = csv.writer(csvfile)
        csv_writer.writerow(
            ["File", "Coverage", "Lines To Cover", "Branch Coverage", "Conditions To Cover"])
        csv_writer.writerows(data)


def main(list_file_path):
    rows = []
    with open(list_file_path) as fp:
        line = fp.readline()
        while line:
            print("Fetching Coverage for File {}".format(line.strip()))
            line = fp.readline()
            rows.append(calculate_coverage(line.strip()))
    export_file(rows)


if __name__ == "__main__":
    file_path = sys.argv[1]
    if file_path:
        main(file_path)
    else:
        raise Exception("Not A valid File Path")
