resource "google_logging_metric" "dev_log_initialized_in_the_same_thread" {
  name = join("_", [local.name_prefix, "dev_log_initialized_in_the_same_thread"])
  description = "Owner: Platform commons"
  filter = join("\n", [local.filter_prefix,
    "Initialized in the same thread with a different value"
  ])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type = "INT64"
    labels {
      key = "mdc_key"
      value_type = "STRING"
      description = "The mdc key that is overwritten"
    }
  }
  label_extractors = {
    "mdc_key": "EXTRACT(jsonPayload.harness.MDCKey)"
  }
}

resource "google_logging_metric" "dev_log_same_value_in_mdc_and_messsage" {
  name = join("_", [local.name_prefix, "dev_log_same_value_in_mdc_and_messsage"])
  description = "Owner: Platform commons"
  filter = join("\n", [local.filter_prefix,
    "MDC table and the logging message have the same value"
  ])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type = "INT64"
    labels {
      key = "mdc_key"
      value_type = "STRING"
      description = "The mdc key that is overwritten"
    }
  }
  label_extractors = {
    "mdc_key": "EXTRACT(jsonPayload.harness.MDCKey)"
  }
}