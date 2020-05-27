resource "google_logging_metric" "perpetual_task_delay" {
  name = join("_", [local.name_prefix, "perpetual_task_delay"])
  description = "Owner: Platform commons"
  filter = join("\n",
    [local.filter_prefix,
      "\"first poll from this delegate for task\" OR \"update for task\""])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type = "DISTRIBUTION"
    unit = "ms"
  }
  value_extractor = "EXTRACT(jsonPayload.harness.delay)"
  bucket_options {
    explicit_buckets {
      bounds = [0, 1000, 30000, 60000, 300000, 1500000, 3000000, 6000000]
    }
  }
}