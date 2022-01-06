# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import csv
import os
import requests
import sys

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
    response = requests.get(url, auth=(token, ''))
    return response.json()


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
    if lines_to_cover is None:
        return None
    return [coverage_file_path, coverage, lines_to_cover, branch_coverage, conditions_to_cover]


def export_file(data, teamName):
    with open("out_" + teamName + ".csv", 'w') as csvfile:
        csv_writer = csv.writer(csvfile)
        csv_writer.writerow(
            ["File", "Coverage(%)", "Lines To Cover", "Branch Coverage(%)", "Conditions To Cover"])
        csv_writer.writerows(data)


def main(list_file_path, teamName):
    rows = []
    with open(list_file_path) as fp:
        files = [line.strip() for line in fp]

    for coverage_file in files:
        try:
            print("Fetching Coverage for File {}".format(coverage_file))
            row = calculate_coverage(coverage_file)
            if row is not None:
                rows.append(row)
        except Exception as e:
            print("Error in processing file {}, Error: {}".format(coverage_file, str(e)))
    export_file(rows, teamName)


if __name__ == "__main__":
    file_path = sys.argv[1]
    teamName = sys.argv[2]
    if file_path and teamName:
        main(file_path, teamName)
    else:
        raise Exception("Not A valid File Path")
