resource "google_logging_metric" "ce_failed_data_pipeline_jobs" {
  name = join("_", [local.name_prefix, "ce_failed_data_pipeline_jobs"])
  description = "Number of failed Data Pipeline jobs. Owner: CE"
  filter = join("\n", [
    local.filter_prefix,
    "jsonPayload.logger=\"io.harness.batch.processing.service.impl.BillingDataPipelineHealthStatusServiceImpl\"",
    "jsonPayload.message:\"Transfer Failed:\""
  ])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type = "INT64"
  }
}

resource "google_monitoring_alert_policy" "ce_failed_data_pipeline_jobs" {
  count = var.deployment == "prod" ? 1 : 0
  enabled = var.deployment == "prod" ? true: false
  notification_channels = var.deployment == "prod" ? ["projects/${var.projectId}/notificationChannels/10185135917587539827"] : []
  display_name = join("_", [local.name_prefix, "ce_failed_data_pipeline_jobs"])
  combiner = "OR"
  conditions {
    display_name = "ce_failed_data_pipeline_jobs"
    condition_threshold {
      filter =  "resource.type=\"global\" AND metric.type=\"logging.googleapis.com/user/${google_logging_metric.ce_failed_data_pipeline_jobs.id}\""
      duration = "300s"
      comparison = "COMPARISON_GT"
      threshold_value  = 0
      aggregations {
        alignment_period = "300s"
        per_series_aligner = "ALIGN_COUNT"
      }
    }
  }
}