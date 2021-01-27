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

# PubSub topic for AWS data pipeline. scheduler pushes into this
resource "google_pubsub_topic" "ce-awsdata-topic" {
  name = "ce-awsdata-scheduler"
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

data "archive_file" "ce-awsdata-manifest" {
  type        = "zip"
  output_path = "${path.module}/files/ce-awsdata-manifest.zip"
  source {
    content  = "${file("${path.module}/src/go/awsdata_manifest_main.go")}"
    filename = "main.go"
  }
  source {
    content  = "${file("${path.module}/src/go/go.mod")}"
    filename = "go.mod"
  }
}

data "archive_file" "ce-awsdata" {
  type        = "zip"
  output_path = "${path.module}/files/ce-awsdata.zip"
  source {
    content  = "${file("${path.module}/src/python/awsdata_preagg_main.py")}"
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
  name   = "ce-clusterdata.${data.archive_file.ce-clusterdata.output_md5}.zip"
  bucket = "${google_storage_bucket.bucket1.name}"
  source = "${path.module}/files/ce-clusterdata.zip"
  depends_on = ["data.archive_file.ce-clusterdata"]
}

resource "google_storage_bucket_object" "ce-gcpdata-archive" {
  name = "ce-gcpdata.${data.archive_file.ce-gcpdata.output_md5}.zip"
  bucket = "${google_storage_bucket.bucket1.name}"
  source = "${path.module}/files/ce-gcpdata.zip"
  depends_on = ["data.archive_file.ce-gcpdata"]
}

resource "google_storage_bucket_object" "ce-awsdata-manifest-archive" {
  name = "ce-awsdata-manifest.${data.archive_file.ce-awsdata-manifest.output_md5}.zip"
  bucket = "${google_storage_bucket.bucket1.name}"
  source = "${path.module}/files/ce-awsdata-manifest.zip"
  depends_on = ["data.archive_file.ce-awsdata-manifest"]
}

resource "google_storage_bucket_object" "ce-awsdata-archive" {
  name = "ce-awsdata.${data.archive_file.ce-awsdata.output_md5}.zip"
  bucket = "${google_storage_bucket.bucket1.name}"
  source = "${path.module}/files/ce-awsdata.zip"
  depends_on = ["data.archive_file.ce-awsdata"]
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
      retry = false
    }
  }
}

resource "google_cloudfunctions_function" "ce-awsdata-manifest-function" {
  name                      = "ce-awsdata-manifest-terraform"
  entry_point               = "CreateTable"
  available_memory_mb       = 256
  timeout                   = 540
  runtime                   = "go111"
  project                   = "${var.projectId}"
  region                    = "${var.region}"
  source_archive_bucket     = "${google_storage_bucket.bucket1.name}"
  source_archive_object     = "${google_storage_bucket_object.ce-awsdata-manifest-archive.name}"

  event_trigger {
    event_type = "google.storage.object.finalize"
    resource   = "awscustomerbillingdata-${var.deployment}"
    failure_policy {
      retry = true
    }
  }
}

resource "google_cloudfunctions_function" "ce-awsdata-function" {
  name                      = "ce-awsdata-terraform"
  entry_point               = "main"
  available_memory_mb       = 256
  timeout                   = 540
  runtime                   = "python37"
  project                   = "${var.projectId}"
  region                    = "${var.region}"
  source_archive_bucket     = "${google_storage_bucket.bucket1.name}"
  source_archive_object     = "${google_storage_bucket_object.ce-awsdata-archive.name}"
  #labels = {
  #  deployment_name           = "test"
  #}
  environment_variables = {
    disabled = "false"
    enable_for_accounts = ""
  }
  event_trigger {
    event_type = "google.pubsub.topic.publish"
    resource   = "${google_pubsub_topic.ce-awsdata-topic.name}"
    failure_policy {
      retry = true
    }
  }
}