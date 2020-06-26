resource "google_logging_metric" "ce_graphQL_warnings_errors" {
  name = join("_", [local.name_prefix, "ce_gql_requests"])
  description = "Number of Errors/Warning in graphQL requests received. Owner: CE"
  filter = join("\n", [
    local.filter_prefix,
    "jsonPayload.logger:(\"software.wings.graphql.datafetcher.billing\" OR \"software.wings.graphql.datafetcher.budget\" OR \"software.wings.graphql.datafetcher.ce\" OR \"software.wings.graphql.datafetcher.budget\" OR \"software.wings.graphql.datafetcher.cloudefficiencyevents\" OR \"software.wings.graphql.datafetcher.k8sLabel\")",
    "severity>=WARNING"
  ])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type = "INT64"
    labels {
      key = "accountId"
      value_type = "STRING"
      description = "The accountId"
    }
    labels {
      key = "dataFetcher"
      value_type = "STRING"
      description = "The datafetcher of Warning/Error Origin"
    }
  }
  label_extractors = {
    "accountId" : "EXTRACT(jsonPayload.harness.accountId)"
    "dataFetcher" : "EXTRACT(jsonPayload.logger)"
  }
}