resource "google_logging_metric" "delegate_tasks_queue_time" {
  name = join("_", [local.name_prefix, "delegate_tasks_queue_time"])
  description = "Owner: Platform delegate"
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
      bounds = [0, 1000, 2000, 3000, 5000, 8000, 13000, 21000, 34000, 55000, 89000, 144000, 233000, 377000]
    }
  }
}

resource "google_logging_metric" "delegate_tasks_rebroadcast" {
  name = join("_", [local.name_prefix, "delegate_tasks_rebroadcast"])
  description = "Owner: Platform delegate"
  filter = join("\n", [local.filter_prefix,
    "\"Rebroadcast queued task\""])
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
    "accountId": "EXTRACT(jsonPayload.harness.accountId)"
  }
}

resource "google_logging_metric" "delegate_tasks_response" {
  name = join("_", [local.name_prefix, "delegate_tasks_response"])
  description = "Owner: Platform delegate"
  filter = join("\n", [local.filter_prefix,
    "\"response received for task\""])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type = "INT64"
    labels {
      key = "responseCode"
      value_type = "STRING"
      description = "The responseCode"
    }
  }
  label_extractors = {
    "responseCode": "REGEXP_EXTRACT(jsonPayload.message, \"responseCode \\\\[(.+)\\\\]\")",
  }
}

resource "google_logging_metric" "delegate_tasks_creation" {
  name = join("_", [local.name_prefix, "delegate_tasks_creation"])
  description = "Owner: Platform delegate"
  filter = join("\n", [local.filter_prefix,
    "\"Queueing async\" OR \"Executing sync\""])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type = "INT64"
    labels {
      key = "syncAsync"
      value_type = "STRING"
      description = "Is the task sync or async"
    }
    labels {
      key = "taskType"
      value_type = "STRING"
      description = "The type of the task"
    }
    labels {
      key = "taskGroup"
      value_type = "STRING"
      description = "The group of the task"
    }
  }
  label_extractors = {
    "syncAsync": "REGEXP_EXTRACT(jsonPayload.message, \"(sync|async)\")",
    "taskType": "EXTRACT(jsonPayload.harness.taskType)",
    "taskGroup": "EXTRACT(jsonPayload.harness.taskGroup)"
  }
}

resource "google_logging_metric" "delegate_tasks_acquire_by_owner" {
  name = join("_", [local.name_prefix, "delegate_tasks_acquire_by_owner"])
  description = "Owner: Platform delegate"
  filter = join("\n", [local.filter_prefix,
    "\"Acquiring delegate task\""])
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
    "accountId": "EXTRACT(jsonPayload.harness.accountId)"
  }
}

resource "google_logging_metric" "delegate_tasks_acquire_by_type" {
  name = join("_", [local.name_prefix, "delegate_tasks_acquire_by_type"])
  description = "Owner: Platform delegate"
  filter = join("\n", [local.filter_prefix,
    "\"eligible to execute task\""])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type = "INT64"
    labels {
      key = "taskType"
      value_type = "STRING"
      description = "The type of the task"
    }
    labels {
      key = "taskGroup"
      value_type = "STRING"
      description = "The group of the task"
    }
  }
  label_extractors = {
    "taskType": "EXTRACT(jsonPayload.harness.taskType)",
    "taskGroup": "EXTRACT(jsonPayload.harness.taskGroup)"
  }
}

resource "google_logging_metric" "delegate_tasks_validate" {
  name = join("_", [local.name_prefix, "delegate_tasks_validate"])
  description = "Owner: Platform delegate"
  filter = join("\n", [local.filter_prefix,
    "\"Delegate to validate\""])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type = "INT64"
    labels {
      key = "taskType"
      value_type = "STRING"
      description = "The type of the task"
    }
    labels {
      key = "taskGroup"
      value_type = "STRING"
      description = "The group of the task"
    }
  }
  label_extractors = {
    "taskType": "EXTRACT(jsonPayload.harness.taskType)",
    "taskGroup": "EXTRACT(jsonPayload.harness.taskGroup)"
  }
}

resource "google_logging_metric" "delegate_tasks_assigning" {
  name = join("_", [local.name_prefix, "delegate_tasks_assigning"])
  description = "Owner: Platform delegate"
  filter = join("\n", [local.filter_prefix,
    "\"Assigning sync task to delegate\" OR \"Assigning async task to delegate\""])
  metric_descriptor {
    metric_kind = "DELTA"
    value_type = "INT64"
    labels {
      key = "taskType"
      value_type = "STRING"
      description = "The type of the task"
    }
    labels {
      key = "taskGroup"
      value_type = "STRING"
      description = "The group of the task"
    }
  }
  label_extractors = {
    "taskType": "EXTRACT(jsonPayload.harness.taskType)",
    "taskGroup": "EXTRACT(jsonPayload.harness.taskGroup)"
  }
}

resource "google_logging_metric" "delegate_no_eligible" {
  name = join("_", [local.name_prefix, "delegate_no_eligible"])
  description = "Owner: Platform delegate"
  filter = join("\n", [local.filter_prefix,
    "\"delegates active but no delegates are eligible to execute task\""])
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
    "accountId": "EXTRACT(jsonPayload.harness.accountId)"
  }
}