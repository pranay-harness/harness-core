# bucket for cloudfunctions zip storage
resource "google_storage_bucket" "bucket1" {
  name = "ce-functions-deploy-${var.deployment}"
  project = "${var.projectId}"
}


# PubSub topic for GCP data pipeline
resource "google_pubsub_topic" "ce-gcpdata-topic" {
  name = "ce-gcpdata"
  project = "${var.projectId}"
}

data "archive_file" "ce-clusterdata" {
  type        = "zip"
  output_path = "${path.module}/files/ce-clusterdata.zip"
  source {
    content  = "${file("${path.module}/src/python/clusterdata_main.py")}"
    filename = "main.py"
  }
  source {
    content  = "${file("${path.module}/src/python/clusterdata_schema.py")}"
    filename = "clusterdata_schema.py"
  }
  source {
    content  = "${file("${path.module}/src/python/unified_schema.py")}"
    filename = "unified_schema.py"
  }
  source {
    content  = "${file("${path.module}/src/python/preaggregated_schema.py")}"
    filename = "preaggregated_schema.py"
  }
  source {
    content  = "${file("${path.module}/src/python/util.py")}"
    filename = "util.py"
  }
  source {
    content  = "${file("${path.module}/src/python/requirements.txt")}"
    filename = "requirements.txt"
  }
}

data "archive_file" "ce-gcpdata" {
  type        = "zip"
  output_path = "${path.module}/files/ce-gcpdata.zip"
  source {
    content  = "${file("${path.module}/src/python/gcpdata_main.py")}"
    filename = "main.py"
  }
  source {
    content  = "${file("${path.module}/src/python/clusterdata_schema.py")}"
    filename = "clusterdata_schema.py"
  }
  source {
    content  = "${file("${path.module}/src/python/unified_schema.py")}"
    filename = "unified_schema.py"
  }
  source {
    content  = "${file("${path.module}/src/python/preaggregated_schema.py")}"
    filename = "preaggregated_schema.py"
  }
  source {
    content  = "${file("${path.module}/src/python/util.py")}"
    filename = "util.py"
  }
  source {
    content  = "${file("${path.module}/src/python/requirements.txt")}"
    filename = "requirements.txt"
  }
}

resource "google_storage_bucket_object" "ce-clusterdata-archive" {
  name   = "ce-clusterdata.zip"
  bucket = "${google_storage_bucket.bucket1.name}"
  source = "${path.module}/files/ce-clusterdata.zip"
  depends_on = ["data.archive_file.ce-clusterdata"]
}

resource "google_storage_bucket_object" "ce-gcpdata-archive" {
  name   = "ce-gcpdata.zip"
  bucket = "${google_storage_bucket.bucket1.name}"
  source = "${path.module}/files/ce-gcpdata.zip"
  depends_on = ["data.archive_file.ce-gcpdata"]
}

resource "google_cloudfunctions_function" "ce-clusterdata-function" {
    name                      = "ce-clusterdata-terraform"
    entry_point               = "main"
    available_memory_mb       = 256
    timeout                   = 540
    runtime                   = "python37"
    project                   = "${var.projectId}"
    region                    = "${var.region}"
    source_archive_bucket     = "${google_storage_bucket.bucket1.name}"
    source_archive_object     = "${google_storage_bucket_object.ce-clusterdata-archive.name}"
    environment_variables = {
      disabled = "false"
      disable_for_accounts = ""
    }
    event_trigger {
      event_type = "google.storage.object.finalize"
      resource   = "clusterdata-${var.deployment}"
      failure_policy {
        retry = true
      }
    }
}

resource "google_cloudfunctions_function" "ce-gcpdata-function" {
  name                      = "ce-gcpdata-terraform"
  entry_point               = "main"
  available_memory_mb       = 256
  timeout                   = 540
  runtime                   = "python37"
  project                   = "${var.projectId}"
  region                    = "${var.region}"
  source_archive_bucket     = "${google_storage_bucket.bucket1.name}"
  source_archive_object     = "${google_storage_bucket_object.ce-gcpdata-archive.name}"
  #labels = {
  #  deployment_name           = "test"
  #}
  environment_variables = {
    disabled = "false"
    disable_for_accounts = ""
  }
  event_trigger {
    event_type = "google.pubsub.topic.publish"
    resource   = "${google_pubsub_topic.ce-gcpdata-topic.name}"
    failure_policy {
      retry = true
    }
  }
}