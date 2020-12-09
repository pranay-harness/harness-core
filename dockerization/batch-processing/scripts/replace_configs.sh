#!/usr/bin/env bash

CONFIG_FILE=/opt/harness/batch-processing-config.yml

if [[ "" != "$MONGO_URI" ]]; then
  yq write -i $CONFIG_FILE harness-mongo.uri "$MONGO_URI"
fi

if [[ "" != "$MONGO_READ_PREF_NAME" ]]; then
  yq write -i $CONFIG_FILE harness-mongo.readPref.name "$MONGO_READ_PREF_NAME"
fi

if [[ "" != "$MONGO_READ_PREF_TAGS" ]]; then
  IFS=',' read -ra TAG_ITEMS <<< "$MONGO_READ_PREF_TAGS"
  for ITEM in "${TAG_ITEMS[@]}"; do
    TAG_NAME=$(echo $ITEM | awk -F= '{print $1}')
    TAG_VALUE=$(echo $ITEM | awk -F= '{print $2}')
    yq write -i $CONFIG_FILE "harness-mongo.readPref.tagSet.[$TAG_NAME]" "$TAG_VALUE"
  done
fi

if [[ "" != "$MONGO_INDEX_MANAGER_MODE" ]]; then
  yq write -i $CONFIG_FILE harness-mongo.indexManagerMode $MONGO_INDEX_MANAGER_MODE
fi

if [[ "" != "$EVEMTS_MONGO_INDEX_MANAGER_MODE" ]]; then
  yq write -i $CONFIG_FILE events-mongo.indexManagerMode $EVEMTS_MONGO_INDEX_MANAGER_MODE
fi

if [[ "" != "$EVENTS_MONGO_URI" ]]; then
  yq write -i $CONFIG_FILE events-mongo.uri "$EVENTS_MONGO_URI"
fi

if [[ "" != "$TIMESCALEDB_URI" ]]; then
  yq write -i $CONFIG_FILE timescaledb.timescaledbUrl "$TIMESCALEDB_URI"
fi

if [[ "" != "$TIMESCALEDB_USERNAME" ]]; then
  yq write -i $CONFIG_FILE timescaledb.timescaledbUsername "$TIMESCALEDB_USERNAME"
fi

if [[ "" != "$TIMESCALEDB_PASSWORD" ]]; then
  yq write -i $CONFIG_FILE timescaledb.timescaledbPassword "$TIMESCALEDB_PASSWORD"
fi

if [[ "" != "$S3_SYNC_CONFIG_BUCKET_NAME" ]]; then
  yq write -i $CONFIG_FILE awsS3SyncConfig.awsS3BucketName "$S3_SYNC_CONFIG_BUCKET_NAME"
fi

if [[ "" != "$QUERY_BATCH_SIZE" ]]; then
  yq write -i $CONFIG_FILE batchQueryConfig.queryBatchSize "$QUERY_BATCH_SIZE"
fi

if [[ "" != "$SYNC_JOB_DISABLED" ]]; then
  yq write -i $CONFIG_FILE batchQueryConfig.syncJobDisabled "$SYNC_JOB_DISABLED"
fi

if [[ "" != "$S3_SYNC_CONFIG_ACCESSKEY" ]]; then
  yq write -i $CONFIG_FILE awsS3SyncConfig.awsAccessKey "$S3_SYNC_CONFIG_ACCESSKEY"
fi

if [[ "" != "$S3_SYNC_CONFIG_SECRETKEY" ]]; then
  yq write -i $CONFIG_FILE awsS3SyncConfig.awsSecretKey "$S3_SYNC_CONFIG_SECRETKEY"
fi

if [[ "" != "$S3_SYNC_CONFIG_REGION" ]]; then
  yq write -i $CONFIG_FILE awsS3SyncConfig.region "$S3_SYNC_CONFIG_REGION"
fi

if [[ "" != "$DATA_PIPELINE_CONFIG_GCP_PROJECT_ID" ]]; then
  yq write -i $CONFIG_FILE billingDataPipelineConfig.gcpProjectId "$DATA_PIPELINE_CONFIG_GCP_PROJECT_ID"
fi

if [[ "" != "$DATA_PIPELINE_CONFIG_GCS_BASE_PATH" ]]; then
  yq write -i $CONFIG_FILE billingDataPipelineConfig.gcsBasePath "$DATA_PIPELINE_CONFIG_GCS_BASE_PATH"
fi

if [[ "" != "$GCP_PIPELINE_PUB_SUB_TOPIC" ]]; then
  yq write -i $CONFIG_FILE billingDataPipelineConfig.gcpPipelinePubSubTopic "$GCP_PIPELINE_PUB_SUB_TOPIC"
fi

if [[ "" != "$GCP_USE_NEW_PIPELINE" ]]; then
  yq write -i $CONFIG_FILE billingDataPipelineConfig.gcpUseNewPipeline "$GCP_USE_NEW_PIPELINE"
fi

if [[ "" != "$CLUSTER_DATA_GCS_BUCKET" ]]; then
  yq write -i $CONFIG_FILE billingDataPipelineConfig.clusterDataGcsBucketName "$CLUSTER_DATA_GCS_BUCKET"
fi

if [[ "" != "$CLUSTER_DATA_GCS_BACKUP_BUCKET" ]]; then
  yq write -i $CONFIG_FILE billingDataPipelineConfig.clusterDataGcsBackupBucketName "$CLUSTER_DATA_GCS_BACKUP_BUCKET"
fi

if [[ "" != "$SMTP_HOST" ]]; then
  yq write -i $CONFIG_FILE smtp.host "$SMTP_HOST"
fi

if [[ "" != "$SMTP_USERNAME" ]]; then
  yq write -i $CONFIG_FILE smtp.username "$SMTP_USERNAME"
fi

if [[ "" != "$SMTP_PASSWORD" ]]; then
  yq write -i $CONFIG_FILE smtp.password "$SMTP_PASSWORD"
fi

if [[ "" != "$UI_SERVER_URL" ]]; then
  yq write -i $CONFIG_FILE baseUrl "$UI_SERVER_URL"
fi

if [[ "" != "$SEGMENT_ENABLED" ]]; then
  yq write -i $CONFIG_FILE segmentConfig.enabled "$SEGMENT_ENABLED"
fi

if [[ "" != "$SEGMENT_APIKEY" ]]; then
  yq write -i $CONFIG_FILE segmentConfig.apiKey "$SEGMENT_APIKEY"
fi

if [[ "" != "$POD_NAME" ]]; then
  yq write -i $CONFIG_FILE podInfo.name "$POD_NAME"
fi

if [[ "" != "$REPLICA" ]]; then
  yq write -i $CONFIG_FILE podInfo.replica "$REPLICA"
fi

if [[ "" != "$ISOLATED_REPLICA" ]]; then
  yq write -i $CONFIG_FILE podInfo.isolatedReplica "$ISOLATED_REPLICA"
fi

if [[ "" != "$BUDGET_ALERTS_JOB_CRON" ]]; then
  yq write -i $CONFIG_FILE scheduler-jobs-config.budgetAlertsJobCron "$BUDGET_ALERTS_JOB_CRON"
fi

if [[ "" != "$WEEKLY_REPORT_JOB_CRON" ]]; then
  yq write -i $CONFIG_FILE scheduler-jobs-config.weeklyReportsJobCron "$WEEKLY_REPORT_JOB_CRON"
fi