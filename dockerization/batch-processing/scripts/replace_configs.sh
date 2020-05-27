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

if [[ "" != "$SMTP_HOST" ]]; then
  yq write -i $CONFIG_FILE smtp.host "$SMTP_HOST"
fi

if [[ "" != "$SMTP_USERNAME" ]]; then
  yq write -i $CONFIG_FILE smtp.username "$SMTP_USERNAME"
fi

if [[ "" != "$SMTP_PASSWORD" ]]; then
  yq write -i $CONFIG_FILE smtp.password "$SMTP_PASSWORD"
fi