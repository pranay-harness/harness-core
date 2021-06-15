#!/usr/bin/env python
import yaml
import glob
import json
import itertools
metric_def_file_paths = glob.glob("../main/resources/metrics/metricDefinitions/*.yaml")
metric_def_file_paths = itertools.chain(list(metric_def_file_paths), ["./runtime_metric_definitions.yaml"])
count_metric_template = None
lastvalue_metric_template = None
duration_metric_template = None
output_json_strings_list = []
def metric_template_json(template, metric_name, title):
  return template.replace("$metric_name", metric_name).replace("$title", title)
with open('count_metric_template.json', 'r') as file:
  count_metric_template = file.read()
with open('lastvalue_metric_template.json', 'r') as file:
  lastvalue_metric_template = file.read()
with open('duration_metric_template.json', 'r') as file:
  duration_metric_template = file.read()

for metric_def_file_path in metric_def_file_paths:
  print(metric_def_file_path)
  with open(metric_def_file_path, 'r') as stream:
    try:
      metric_defs = yaml.safe_load(stream)
      for metric in metric_defs['metrics']:
        if (metric["type"] == "Count"):
          output_json_strings_list.append(json.loads(metric_template_json(count_metric_template, metric['metricName'], metric_defs['name']
                                                             + " - " + metric['metricName'])))
        if (metric["type"] == "LastValue"):
          output_json_strings_list.append(json.loads(metric_template_json(lastvalue_metric_template, metric['metricName'], metric_defs['name']
                                                                          + " - " + metric['metricName'])))
        if (metric["type"] == "Duration"):
          output_json_strings_list.append(json.loads(metric_template_json(duration_metric_template, metric['metricName'], metric_defs['name']
                                                                          + " - " + metric['metricName'])))
    except yaml.YAMLError as exc:
      print(exc)

tasks_template = None
with open('tasks_template.tf', 'r') as file:
  tasks_template = file.read()
tasks_tf = tasks_template.replace("$json_array", json.dumps(output_json_strings_list, indent=2))
task_tf_file = open("./../../../scripts/terraform/stackdriver/cvng/dashboard.tf", "w+")
task_tf_file.write(tasks_tf)
task_tf_file.close()
print(output_json_strings_list)