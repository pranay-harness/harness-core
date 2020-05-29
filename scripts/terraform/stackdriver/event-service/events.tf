resource "google_logging_metric" "ce_publish_requests" {
  name = join("_", [local.name_prefix, "ce_publish_requests"])
  description = "Number of publish requests received. Owner: CE"
  filter = join("\n", [
    local.filter_prefix,
    "jsonPayload.logger=\"io.harness.event.grpc.EventPublisherServerImpl\"",
    "jsonPayload.message=\"Received publish request\""
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
