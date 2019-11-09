resource "google_logging_metric" "iterators_working_on_entity_by_thread_pool" {
  name = join("_", [local.name_prefix, "iterators_working_on_entity"])
  filter = join("\n", [local.filter_prefix,
    "\"Working on entity\""])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type = "INT64"
    labels {
      key = "thread_pool"
      value_type = "STRING"
      description = "The thread pool the entity is processed in"
    }
  }
  label_extractors = {
    "thread_pool": "EXTRACT(jsonPayload.thread)",
  }
}

resource "google_logging_metric" "iterators_delays" {
  name = join("_", [local.name_prefix, "iterators_delays"])
  filter = join("\n", [
    local.filter_prefix,
    "\"Working on entity\""])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type = "DISTRIBUTION"
    unit = "ms"
    labels {
      key = "thread_pool"
      value_type = "STRING"
      description = "The class of the entity to operate over"
    }
  }
  value_extractor = "EXTRACT(jsonPayload.harness.delay)"
  bucket_options {
    explicit_buckets {
      bounds = [1000, 30000, 60000, 300000, 1500000, 3000000, 6000000]
    }
  }
  label_extractors = {
    "thread_pool": "EXTRACT(jsonPayload.thread)"
  }
}


resource "google_logging_metric" "iterators_issues" {
  name = join("_", [local.name_prefix, "iterators_issues"])
  filter = join("\n", [local.filter_prefix,
    "MongoPersistenceIterator",
    "severity=\"ERROR\""])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type = "INT64"
    labels {
      key = "thread_pool"
      value_type = "STRING"
      description = "The class of the entity to operate over"
    }
  }
  label_extractors = {
    "thread_pool": "EXTRACT(jsonPayload.thread)"
  }
}