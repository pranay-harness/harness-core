resource "google_logging_metric" "delegate_tasks_queue_time" {
  name = join("_", [local.name_prefix, "delegate_tasks_queue_time"])
  filter = join("\n", [local.filter_prefix,
    "\"Task assigned to delegate\""])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type = "DISTRIBUTION"
    unit = "ms"
  }
  value_extractor = "EXTRACT(jsonPayload.harness.delay)"
  bucket_options {
    explicit_buckets {
      bounds = [1000, 2000, 3000, 5000, 8000, 13000, 21000, 34000, 55000]
    }
  }
}