resource "google_logging_metric" "wait_engine_duplicate_issues" {
  name = join("_", [local.name_prefix, "wait_engine_duplicate_issues"])
  description = "Owner: CDC orchestration"
  filter = join("\n", [local.filter_prefix,
    "Unexpected rate of DuplicateKeyException per correlation"])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type = "INT64"
    labels {
      key = "delegate"
      value_type = "STRING"
      description = "The class of the entity to operate over"
    }
  }
  label_extractors = {
    "delegate": "EXTRACT(jsonPayload.harness.delegateId)"
  }
}