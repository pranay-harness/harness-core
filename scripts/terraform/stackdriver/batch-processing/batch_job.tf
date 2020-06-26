resource "google_logging_metric" "ce_failed_batch_job" {
  name = join("_", [local.name_prefix, "ce_failed_batch_jobs"])
  description = "Number of failed batch job. Owner: CE"
  filter = join("\n", [
    local.filter_prefix,
    "jsonPayload.logger=\"io.harness.batch.processing.schedule.BatchJobRunner\"",
    "jsonPayload.message:\"Error while running batch job\""
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
    "accountId" : "EXTRACT(jsonPayload.harness.accountId)"
  }
}

resource "google_monitoring_alert_policy" "ce_failed_batch_job" {
  count = var.deployment == "prod" ? 1 : 0
  enabled = var.deployment == "prod" ? true: false
  notification_channels = var.deployment == "prod" ? ["projects/${var.projectId}/notificationChannels/10185135917587539827"] : []
  display_name = join("_", [local.name_prefix, "ce_failed_batch_job"])
  combiner = "OR"
  conditions {
    display_name = "ce_failed_batch_job"
    condition_threshold {
      filter =  "resource.type=\"global\" AND metric.type=\"logging.googleapis.com/user/${google_logging_metric.ce_failed_batch_job.id}\""
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
