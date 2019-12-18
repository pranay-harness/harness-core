resource "google_logging_metric" "queues_working_on_message_by_thread_pool" {
  name = join("_", [local.name_prefix, "queues_working_on_message"])
  filter = join("\n", [local.filter_prefix,
    "\"Working on message\""])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type = "INT64"
    labels {
      key = "thread_pool"
      value_type = "STRING"
      description = "The thread pool the message is processed in"
    }
    labels {
      key = "topic"
      value_type = "STRING"
      description = "The topic the message has"
    }
  }
  label_extractors = {
    "thread_pool": "REGEXP_EXTRACT(jsonPayload.thread, \"(.*).......................\")",
    "topic": "EXTRACT(jsonPayload.harness.messageTopic)",
  }
}

resource "google_logging_metric" "queues_delays" {
  name = join("_", [local.name_prefix, "queues_delays"])
  filter = join("\n", [
    local.filter_prefix,
    "\"Working on message\""])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type = "DISTRIBUTION"
    unit = "ms"
    labels {
      key = "thread_pool"
      value_type = "STRING"
      description = "The thread pool the message is processed in"
    }
  }
  value_extractor = "EXTRACT(jsonPayload.harness.delay)"
  bucket_options {
    explicit_buckets {
      bounds = [1000, 30000, 60000, 300000, 1500000, 3000000, 6000000]
    }
  }
  label_extractors = {
    "thread_pool": "REGEXP_EXTRACT(jsonPayload.thread, \"(.*).......................\")",
  }
}

resource "google_logging_metric" "queues_process_time" {
  name = join("_", [local.name_prefix, "queues_process_time"])
  filter = join("\n", [
    local.filter_prefix,
    "\"Done with message\""])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type = "DISTRIBUTION"
    unit = "ms"
    labels {
      key = "thread_pool"
      value_type = "STRING"
      description = "The thread pool the message is processed in"
    }
  }
  value_extractor = "EXTRACT(jsonPayload.harness.processTime)"
  bucket_options {
    explicit_buckets {
      bounds = [1000, 30000, 60000, 300000, 1500000, 3000000, 6000000]
    }
  }
  label_extractors = {
    "thread_pool": "REGEXP_EXTRACT(jsonPayload.thread, \"(.*).......................\")",
  }
}

resource "google_logging_metric" "queues_issues" {
  name = join("_", [local.name_prefix, "queues_issues"])
  filter = join("\n", [local.filter_prefix,
    "QueueListener",
    "severity=\"ERROR\""])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type = "INT64"
    labels {
      key = "thread_pool"
      value_type = "STRING"
      description = "The thread pool the message is processed in"
    }
  }
  label_extractors = {
    "thread_pool": "REGEXP_EXTRACT(jsonPayload.thread, \"(.*).......................\")",
  }
}