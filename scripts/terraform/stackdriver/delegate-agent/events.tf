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

resource "google_monitoring_alert_policy" "ce_event_drops" {
  count = var.deployment == "prod" ? 1 : 0
  enabled = var.deployment == "prod" ? true: false
  notification_channels = var.deployment == "prod" ? ["projects/${var.projectId}/notificationChannels/10185135917587539827"] : []
  display_name = join("_", [local.name_prefix, "ce_event_drops"])
  combiner = "OR"
  conditions {
    display_name = "ce_event_drop"
    condition_threshold {
      filter =  "resource.type=\"global\" AND metric.type=\"logging.googleapis.com/user/${local.name_prefix}_ce_event_drops\""
      duration = "60s"
      comparison = "COMPARISON_GT"
      aggregations {
        group_by_fields = ["metric.labels.accountId"]
        alignment_period = "60s"
        per_series_aligner = "ALIGN_COUNT"
      }
    }
  }
}
