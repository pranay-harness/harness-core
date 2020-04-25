resource "google_logging_metric" "dev_db_index_issues" {
  name = join("_", [local.name_prefix, "dev_db_index_issues"])
  description = "Owner: Platform commons"
  filter = join("\n", [local.filter_prefix,
    "(\"IndexManager\" OR \"HObjectFactory\")",
    "severity=\"ERROR\""
  ])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type = "INT64"
  }
}

resource "google_logging_metric" "dev_db_native_iterator_issues" {
  name = join("_", [local.name_prefix, "dev_db_native_iterator_issues"])
  filter = join("\n", [local.filter_prefix,
    "(\"Do not use the query as iterator directly.\")",
    "severity=\"ERROR\""
  ])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type = "INT64"
  }
}