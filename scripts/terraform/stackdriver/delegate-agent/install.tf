resource "google_logging_metric" "starting" {
  name = join("_", [local.name_prefix, "starting"])
  description = "Owner: Platform delegate"
  filter = join("\n", [local.filter_prefix,
    "\"Starting Delegate\""])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type = "INT64"
    labels {
      key = "version"
      value_type = "STRING"
      description = "Per version"
    }
  }
  label_extractors = {
    "version": "EXTRACT(labels.version)"
  }
}