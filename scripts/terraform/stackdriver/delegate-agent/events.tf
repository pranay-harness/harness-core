resource "google_logging_metric" "ce_event_drops" {
  name = join("_", [local.name_prefix, "ce_event_drops"])
  description = "Number of events dropped. Owner: CE"
  filter = join("\n", [
    local.filter_prefix,
    "jsonPayload.logger=\"io.harness.event.client.impl.appender.ChronicleEventAppender\"",
    "jsonPayload.message=\"Dropping message as queue is not healthy\""
  ])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type = "INT64"
    labels {
      key = "accountId"
      value_type = "STRING"
      description = "The accountId"
    }
  }
  label_extractors = {
    "accountId" : "EXTRACT(labels.accountId)"
  }
}

