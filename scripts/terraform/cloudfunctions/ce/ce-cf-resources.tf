# bucket for cloudfunctions zip storage
resource "google_storage_bucket" "bucket1" {
  name = "ce-functions-deploy-${var.deployment}"
  project = "${var.projectId}"
}

# PubSub topic for AWS data pipeline. scheduler pushes into this
resource "google_pubsub_topic" "ce-awsdata-topic" {
  name = "ce-awsdata-scheduler"
  project = "${var.projectId}"
}

# PubSub topic for AWS NG data pipeline. AWS GCS data transfer job ingests into this
resource "google_pubsub_topic" "ce-aws-billing-gcs-topic" {
  name = "ce-aws-billing-gcs"
  project = "${var.projectId}"
}

# PubSub topic for AWS NG data pipeline. ce-aws-billing-gcs CF pushes into this
resource "google_pubsub_topic" "ce-aws-billing-cf-topic" {
  name = "ce-aws-billing-cf"
  project = "${var.projectId}"
}

# PubSub topic for AWS EC2 Inventory data pipeline. scheduler pushes into this
resource "google_pubsub_topic" "ce-awsdata-ec2-inventory-topic" {
  name = "ce-awsdata-ec2-inventory-scheduler"
  project = "${var.projectId}"
}

# PubSub topic for AWS EC2 Inventory data pipeline. scheduler pushes into this
resource "google_pubsub_topic" "ce-awsdata-ec2-inventory-load-topic" {
  name = "ce-awsdata-ec2-inventory-load-scheduler"
  project = "${var.projectId}"
}

# PubSub topic for AWS EC2 Inventory metric data pipeline. scheduler pushes into this
resource "google_pubsub_topic" "ce-awsdata-ec2-metric-topic" {
  name = "ce-awsdata-ec2-metric-inventory-scheduler"
  project = "${var.projectId}"
}

# PubSub topic for AWS EBS Inventory data pipeline. scheduler pushes into this
resource "google_pubsub_topic" "ce-awsdata-ebs-inventory-topic" {
  name = "ce-awsdata-ebs-inventory-scheduler"
  project = "${var.projectId}"
}

# PubSub topic for loading AWS EBS Inventory data into main bq table. scheduler pushes into this
resource "google_pubsub_topic" "ce-awsdata-ebs-inventory-load-topic" {
  name = "ce-awsdata-ebs-inventory-load-scheduler"
  project = "${var.projectId}"
}

# PubSub topic for AWS EBS Inventory Metrics data pipeline. scheduler pushes into this
resource "google_pubsub_topic" "ce-awsdata-ebs-metrics-topic" {
  name = "ce-awsdata-ebs-metrics-inventory-scheduler"
  project = "${var.projectId}"
}

# PubSub topic for AZURE data pipeline. CF1 pushes into this
resource "google_pubsub_topic" "ce-azure-billing-cf-topic" {
  name = "ce-azure-billing-cf"
  project = "${var.projectId}"
}

# PubSub topic for AZURE data pipeline. Azure GCS data transfer job ingests into this
resource "google_pubsub_topic" "ce-azure-billing-gcs-topic" {
  name = "ce-azure-billing-gcs"
  project = "${var.projectId}"
}

# PubSub topic for GCP data pipeline
resource "google_pubsub_topic" "ce-gcpdata-topic" {
  name = "ce-gcpdata"
  project = "${var.projectId}"
}

# Archive files keep the CF files and dependencies
data "archive_file" "ce-clusterdata" {
  type        = "zip"
  output_path = "${path.module}/files/ce-clusterdata.zip"
  source {
    content  = "${file("${path.module}/src/python/clusterdata_main.py")}"
    filename = "main.py"
  }
  source {
    content  = "${file("${path.module}/src/python/bq_schema.py")}"
    filename = "bq_schema.py"
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
    content  = "${file("${path.module}/src/python/bq_schema.py")}"
    filename = "bq_schema.py"
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

data "archive_file" "ce-aws-billing-bq" {
  type        = "zip"
  output_path = "${path.module}/files/ce-aws-billing-bq.zip"
  source {
    content  = "${file("${path.module}/src/python/aws_billing_bq_main.py")}"
    filename = "main.py"
  }
  source {
    content  = "${file("${path.module}/src/python/bq_schema.py")}"
    filename = "bq_schema.py"
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

data "archive_file" "ce-aws-billing-gcs" {
  type        = "zip"
  output_path = "${path.module}/files/ce-aws-billing-gcs.zip"
  source {
    content  = "${file("${path.module}/src/python/aws_billing_gcs_main.py")}"
    filename = "main.py"
  }
  source {
    content  = "${file("${path.module}/src/python/requirements.txt")}"
    filename = "requirements.txt"
  }
}

data "archive_file" "ce-azure-billing-gcs" {
  type        = "zip"
  output_path = "${path.module}/files/ce-azure-billing-gcs.zip"
  source {
    content  = "${file("${path.module}/src/python/azure_billing_gcs_main.py")}"
    filename = "main.py"
  }
  source {
    content  = "${file("${path.module}/src/python/requirements.txt")}"
    filename = "requirements.txt"
  }
}

data "archive_file" "ce-azure-billing-bq" {
  type        = "zip"
  output_path = "${path.module}/files/ce-azure-billing-bq.zip"
  source {
    content  = "${file("${path.module}/src/python/azure_billing_bq_main.py")}"
    filename = "main.py"
  }
  source {
    content  = "${file("${path.module}/src/python/bq_schema.py")}"
    filename = "bq_schema.py"
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

data "archive_file" "ce-awsdata-ec2" {
  type        = "zip"
  output_path = "${path.module}/files/ce-awsdata-ec2.zip"
  source {
    content  = "${file("${path.module}/src/python/aws_ec2_data_main.py")}"
    filename = "main.py"
  }
  source {
    content  = "${file("${path.module}/src/python/bq_schema.py")}"
    filename = "bq_schema.py"
  }
  source {
    content  = "${file("${path.module}/src/python/util.py")}"
    filename = "util.py"
  }
  source {
    content  = "${file("${path.module}/src/python/aws_util.py")}"
    filename = "aws_util.py"
  }
  source {
    content  = "${file("${path.module}/src/python/requirements.txt")}"
    filename = "requirements.txt"
  }
}

data "archive_file" "ce-awsdata-ec2-load" {
  type        = "zip"
  output_path = "${path.module}/files/ce-awsdata-ec2-load.zip"
  source {
    content  = "${file("${path.module}/src/python/aws_ec2_data_load.py")}"
    filename = "main.py"
  }
  source {
    content  = "${file("${path.module}/src/python/util.py")}"
    filename = "util.py"
  }
  source {
    content  = "${file("${path.module}/src/python/bq_schema.py")}"
    filename = "bq_schema.py"
  }
  source {
    content  = "${file("${path.module}/src/python/requirements.txt")}"
    filename = "requirements.txt"
  }
}

data "archive_file" "ce-awsdata-ec2-metric" {
  type        = "zip"
  output_path = "${path.module}/files/ce-awsdata-ec2-metric.zip"
  source {
    content  = "${file("${path.module}/src/python/aws_ec2_metric_data_main.py")}"
    filename = "main.py"
  }
  source {
    content  = "${file("${path.module}/src/python/bq_schema.py")}"
    filename = "bq_schema.py"
  }
  source {
    content  = "${file("${path.module}/src/python/aws_util.py")}"
    filename = "aws_util.py"
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

data "archive_file" "ce-awsdata-ebs" {
  type        = "zip"
  output_path = "${path.module}/files/ce-awsdata-ebs.zip"
  source {
    content  = "${file("${path.module}/src/python/aws_ebs_data_main.py")}"
    filename = "main.py"
  }
  source {
    content  = "${file("${path.module}/src/python/bq_schema.py")}"
    filename = "bq_schema.py"
  }
  source {
    content  = "${file("${path.module}/src/python/util.py")}"
    filename = "util.py"
  }
  source {
    content  = "${file("${path.module}/src/python/aws_util.py")}"
    filename = "aws_util.py"
  }
  source {
    content  = "${file("${path.module}/src/python/requirements.txt")}"
    filename = "requirements.txt"
  }
}

data "archive_file" "ce-awsdata-ebs-load" {
  type        = "zip"
  output_path = "${path.module}/files/ce-awsdata-ebs-load.zip"
  source {
    content  = "${file("${path.module}/src/python/aws_ebs_data_load.py")}"
    filename = "main.py"
  }
  source {
    content  = "${file("${path.module}/src/python/util.py")}"
    filename = "util.py"
  }
  source {
    content  = "${file("${path.module}/src/python/bq_schema.py")}"
    filename = "bq_schema.py"
  }
  source {
    content  = "${file("${path.module}/src/python/requirements.txt")}"
    filename = "requirements.txt"
  }
}

data "archive_file" "ce-awsdata-ebs-metrics" {
  type        = "zip"
  output_path = "${path.module}/files/ce-awsdata-ebs-metrics.zip"
  source {
    content  = "${file("${path.module}/src/python/aws_ebs_metrics_data_main.py")}"
    filename = "main.py"
  }
  source {
    content  = "${file("${path.module}/src/python/bq_schema.py")}"
    filename = "bq_schema.py"
  }
  source {
    content  = "${file("${path.module}/src/python/util.py")}"
    filename = "util.py"
  }
  source {
    content  = "${file("${path.module}/src/python/aws_util.py")}"
    filename = "aws_util.py"
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

resource "google_storage_bucket_object" "ce-aws-billing-bq-archive" {
  name = "ce-aws-billing-bq.${data.archive_file.ce-aws-billing-bq.output_md5}.zip"
  bucket = "${google_storage_bucket.bucket1.name}"
  source = "${path.module}/files/ce-aws-billing-bq.zip"
  depends_on = ["data.archive_file.ce-aws-billing-bq"]
}

resource "google_storage_bucket_object" "ce-aws-billing-gcs-archive" {
  name = "ce-aws-billing-gcs.${data.archive_file.ce-aws-billing-gcs.output_md5}.zip"
  bucket = "${google_storage_bucket.bucket1.name}"
  source = "${path.module}/files/ce-aws-billing-gcs.zip"
  depends_on = ["data.archive_file.ce-aws-billing-gcs"]
}

resource "google_storage_bucket_object" "ce-azure-billing-gcs-archive" {
  name = "ce-azure-billing.${data.archive_file.ce-azure-billing-gcs.output_md5}.zip"
  bucket = "${google_storage_bucket.bucket1.name}"
  source = "${path.module}/files/ce-azure-billing-gcs.zip"
  depends_on = ["data.archive_file.ce-azure-billing-gcs"]
}

resource "google_storage_bucket_object" "ce-azure-billing-bq-archive" {
  name = "ce-azure-billing.${data.archive_file.ce-azure-billing-bq.output_md5}.zip"
  bucket = "${google_storage_bucket.bucket1.name}"
  source = "${path.module}/files/ce-azure-billing-bq.zip"
  depends_on = ["data.archive_file.ce-azure-billing-bq"]
}

resource "google_storage_bucket_object" "ce-awsdata-ec2-archive" {
  name = "ce-awsdata.${data.archive_file.ce-awsdata-ec2.output_md5}.zip"
  bucket = "${google_storage_bucket.bucket1.name}"
  source = "${path.module}/files/ce-awsdata-ec2.zip"
  depends_on = ["data.archive_file.ce-awsdata-ec2"]
}

resource "google_storage_bucket_object" "ce-awsdata-ec2-load-archive" {
  name = "ce-awsdata.${data.archive_file.ce-awsdata-ec2-load.output_md5}.zip"
  bucket = "${google_storage_bucket.bucket1.name}"
  source = "${path.module}/files/ce-awsdata-ec2-load.zip"
  depends_on = ["data.archive_file.ce-awsdata-ec2-load"]
}

resource "google_storage_bucket_object" "ce-awsdata-ec2-metric-archive" {
  name = "ce-awsdata.${data.archive_file.ce-awsdata-ec2-metric.output_md5}.zip"
  bucket = "${google_storage_bucket.bucket1.name}"
  source = "${path.module}/files/ce-awsdata-ec2-metric.zip"
  depends_on = ["data.archive_file.ce-awsdata-ec2-metric"]
}

resource "google_storage_bucket_object" "ce-awsdata-ebs-archive" {
  name = "ce-awsdata.${data.archive_file.ce-awsdata-ebs.output_md5}.zip"
  bucket = "${google_storage_bucket.bucket1.name}"
  source = "${path.module}/files/ce-awsdata-ebs.zip"
  depends_on = ["data.archive_file.ce-awsdata-ebs"]
}

resource "google_storage_bucket_object" "ce-awsdata-ebs-load-archive" {
  name = "ce-awsdata.${data.archive_file.ce-awsdata-ebs-load.output_md5}.zip"
  bucket = "${google_storage_bucket.bucket1.name}"
  source = "${path.module}/files/ce-awsdata-ebs-load.zip"
  depends_on = ["data.archive_file.ce-awsdata-ebs-load"]
}

resource "google_storage_bucket_object" "ce-awsdata-ebs-metrics-archive" {
  name = "ce-awsdata.${data.archive_file.ce-awsdata-ebs-metrics.output_md5}.zip"
  bucket = "${google_storage_bucket.bucket1.name}"
  source = "${path.module}/files/ce-awsdata-ebs-metrics.zip"
  depends_on = ["data.archive_file.ce-awsdata-ebs-metrics"]
}

resource "google_cloudfunctions_function" "ce-clusterdata-function" {
    name                      = "ce-clusterdata-terraform"
    entry_point               = "main"
    available_memory_mb       = 256
    timeout                   = 540
    runtime                   = "python38"
    project                   = "${var.projectId}"
    region                    = "${var.region}"
    source_archive_bucket     = "${google_storage_bucket.bucket1.name}"
    source_archive_object     = "${google_storage_bucket_object.ce-clusterdata-archive.name}"
    environment_variables = {
      disabled = "false"
      disable_for_accounts = ""
      GCP_PROJECT = "${var.projectId}"
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
  description               = ""
  entry_point               = "main"
  available_memory_mb       = 256
  timeout                   = 540
  runtime                   = "python38"
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
    GCP_PROJECT = "${var.projectId}"
  }
  event_trigger {
    event_type = "google.pubsub.topic.publish"
    resource   = "${google_pubsub_topic.ce-gcpdata-topic.name}"
    failure_policy {
      retry = false
    }
  }
}

resource "google_cloudfunctions_function" "ce-aws-billing-bq-function" {
  name                      = "ce-aws-billing-bq-terraform"
  description               = ""
  entry_point               = "main"
  available_memory_mb       = 256
  timeout                   = 540
  runtime                   = "python38"
  project                   = "${var.projectId}"
  region                    = "${var.region}"
  source_archive_bucket     = "${google_storage_bucket.bucket1.name}"
  source_archive_object     = "${google_storage_bucket_object.ce-aws-billing-bq-archive.name}"
  #labels = {
  #  deployment_name           = "test"
  #}
  environment_variables = {
    disabled = "false"
    enable_for_accounts = ""
    GCP_PROJECT = "${var.projectId}"
  }
  event_trigger {
    event_type = "google.pubsub.topic.publish"
    resource   = "${google_pubsub_topic.ce-aws-billing-cf-topic.name}"
    failure_policy {
      retry = false
    }
  }
}

resource "google_cloudfunctions_function" "ce-aws-billing-gcs-function" {
  name                      = "ce-aws-billing-gcs-terraform"
  description               = ""
  entry_point               = "main"
  available_memory_mb       = 256
  timeout                   = 540
  runtime                   = "python38"
  project                   = "${var.projectId}"
  region                    = "${var.region}"
  source_archive_bucket     = "${google_storage_bucket.bucket1.name}"
  source_archive_object     = "${google_storage_bucket_object.ce-aws-billing-gcs-archive.name}"

  environment_variables = {
    disabled = "false"
    enable_for_accounts = ""
    GCP_PROJECT = "${var.projectId}"
    AWSCFTOPIC = "${google_pubsub_topic.ce-aws-billing-cf-topic.name}"
  }
  event_trigger {
    event_type = "google.pubsub.topic.publish"
    resource   = "${google_pubsub_topic.ce-aws-billing-gcs-topic.name}"
    failure_policy {
      retry = false
    }
  }
}

resource "google_cloudfunctions_function" "ce-azure-billing-bq-function" {
  name                      = "ce-azure-billing-bq-terraform"
  description               = "This cloudfunction gets triggered when cloud scheduler sends an event in pubsub topic"
  entry_point               = "main"
  available_memory_mb       = 256
  timeout                   = 540
  runtime                   = "python38"
  project                   = "${var.projectId}"
  region                    = "${var.region}"
  source_archive_bucket     = "${google_storage_bucket.bucket1.name}"
  source_archive_object     = "${google_storage_bucket_object.ce-azure-billing-bq-archive.name}"

  environment_variables = {
    disabled = "false"
    enable_for_accounts = ""
    GCP_PROJECT = "${var.projectId}"
  }
  event_trigger {
    event_type = "google.pubsub.topic.publish"
    resource   = "${google_pubsub_topic.ce-azure-billing-cf-topic.name}"
    failure_policy {
      retry = false
    }
  }
}

resource "google_cloudfunctions_function" "ce-azure-billing-gcs-function" {
  name                      = "ce-azure-billing-gcs-terraform"
  description               = "This cloudfunction gets triggered when azure gcs data transfer job completes"
  entry_point               = "main"
  available_memory_mb       = 256
  timeout                   = 540
  runtime                   = "python38"
  project                   = "${var.projectId}"
  region                    = "${var.region}"
  source_archive_bucket     = "${google_storage_bucket.bucket1.name}"
  source_archive_object     = "${google_storage_bucket_object.ce-azure-billing-gcs-archive.name}"

  environment_variables = {
    disabled = "false"
    enable_for_accounts = ""
    GCP_PROJECT = "${var.projectId}"
    AZURECFTOPIC = "${google_pubsub_topic.ce-azure-billing-cf-topic.name}"
  }

  event_trigger {
    event_type = "google.pubsub.topic.publish"
    resource   = "${google_pubsub_topic.ce-azure-billing-gcs-topic.name}"
    failure_policy {
      retry = false
    }
  }
}

resource "google_cloudfunctions_function" "ce-awsdata-ec2-function" {
  name                      = "ce-awsdata-ec2-terraform"
  description               = "This cloudfunction gets triggered upon event in a pubsub topic"
  entry_point               = "main"
  available_memory_mb       = 256
  timeout                   = 540
  runtime                   = "python38"
  project                   = "${var.projectId}"
  region                    = "${var.region}"
  source_archive_bucket     = "${google_storage_bucket.bucket1.name}"
  source_archive_object     = "${google_storage_bucket_object.ce-awsdata-ec2-archive.name}"

  environment_variables = {
    disabled = "false"
    enable_for_accounts = ""
    GCP_PROJECT = "${var.projectId}"
  }

  event_trigger {
    event_type = "google.pubsub.topic.publish"
    resource   = "${google_pubsub_topic.ce-awsdata-ec2-inventory-topic.name}"
    failure_policy {
      retry = false
    }
  }
}

resource "google_cloudfunctions_function" "ce-awsdata-ec2-load-function" {
  name                      = "ce-awsdata-ec2-load-terraform"
  description               = "This cloudfunction gets triggered upon event in a pubsub topic"
  entry_point               = "main"
  available_memory_mb       = 256
  timeout                   = 540
  runtime                   = "python38"
  project                   = "${var.projectId}"
  region                    = "${var.region}"
  source_archive_bucket     = "${google_storage_bucket.bucket1.name}"
  source_archive_object     = "${google_storage_bucket_object.ce-awsdata-ec2-load-archive.name}"

  environment_variables = {
    disabled = "false"
    enable_for_accounts = ""
    GCP_PROJECT = "${var.projectId}"
  }

  event_trigger {
    event_type = "google.pubsub.topic.publish"
    resource   = "${google_pubsub_topic.ce-awsdata-ec2-inventory-load-topic.name}"
    failure_policy {
      retry = false
    }
  }
}

resource "google_cloudfunctions_function" "ce-awsdata-ec2-metric-function" {
  name                      = "ce-awsdata-ec2-metric-terraform"
  description               = "This cloudfunction gets triggered upon event in a pubsub topic"
  entry_point               = "main"
  available_memory_mb       = 256
  timeout                   = 540
  runtime                   = "python38"
  project                   = "${var.projectId}"
  region                    = "${var.region}"
  source_archive_bucket     = "${google_storage_bucket.bucket1.name}"
  source_archive_object     = "${google_storage_bucket_object.ce-awsdata-ec2-metric-archive.name}"

  environment_variables = {
    disabled = "false"
    enable_for_accounts = ""
    GCP_PROJECT = "${var.projectId}"
  }

  event_trigger {
    event_type = "google.pubsub.topic.publish"
    resource   = "${google_pubsub_topic.ce-awsdata-ec2-metric-topic.name}"
    failure_policy {
      retry = false
    }
  }
}

resource "google_cloudfunctions_function" "ce-awsdata-ebs-function" {
  name                      = "ce-awsdata-ebs-terraform"
  description               = "This cloudfunction gets triggered upon event in a pubsub topic"
  entry_point               = "main"
  available_memory_mb       = 256
  timeout                   = 540
  runtime                   = "python38"
  project                   = "${var.projectId}"
  region                    = "${var.region}"
  source_archive_bucket     = "${google_storage_bucket.bucket1.name}"
  source_archive_object     = "${google_storage_bucket_object.ce-awsdata-ebs-archive.name}"

  environment_variables = {
    disabled = "false"
    enable_for_accounts = ""
    GCP_PROJECT = "${var.projectId}"
  }

  event_trigger {
    event_type = "google.pubsub.topic.publish"
    resource   = "${google_pubsub_topic.ce-awsdata-ebs-inventory-topic.name}"
    failure_policy {
      retry = false
    }
  }
}

resource "google_cloudfunctions_function" "ce-awsdata-ebs-load-function" {
  name                      = "ce-awsdata-ebs-load-terraform"
  description               = "This cloudfunction gets triggered upon event in a pubsub topic"
  entry_point               = "main"
  available_memory_mb       = 256
  timeout                   = 540
  runtime                   = "python38"
  project                   = "${var.projectId}"
  region                    = "${var.region}"
  source_archive_bucket     = "${google_storage_bucket.bucket1.name}"
  source_archive_object     = "${google_storage_bucket_object.ce-awsdata-ebs-load-archive.name}"

  environment_variables = {
    disabled = "false"
    enable_for_accounts = ""
    GCP_PROJECT = "${var.projectId}"
  }

  event_trigger {
    event_type = "google.pubsub.topic.publish"
    resource   = "${google_pubsub_topic.ce-awsdata-ebs-inventory-load-topic.name}"
    failure_policy {
      retry = false
    }
  }
}

resource "google_cloudfunctions_function" "ce-awsdata-ebs-metrics-function" {
  name                      = "ce-awsdata-ebs-metrics-terraform"
  description               = "This cloudfunction gets triggered upon event in a pubsub topic"
  entry_point               = "main"
  available_memory_mb       = 256
  timeout                   = 540
  runtime                   = "python38"
  project                   = "${var.projectId}"
  region                    = "${var.region}"
  source_archive_bucket     = "${google_storage_bucket.bucket1.name}"
  source_archive_object     = "${google_storage_bucket_object.ce-awsdata-ebs-metrics-archive.name}"

  environment_variables = {
    disabled = "false"
    enable_for_accounts = ""
    GCP_PROJECT = "${var.projectId}"
  }

  event_trigger {
    event_type = "google.pubsub.topic.publish"
    resource   = "${google_pubsub_topic.ce-awsdata-ebs-metrics-topic.name}"
    failure_policy {
      retry = false
    }
  }
}
