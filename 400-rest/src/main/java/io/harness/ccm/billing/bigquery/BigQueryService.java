package io.harness.ccm.billing.bigquery;

import software.wings.beans.ValidationResult;

import com.google.cloud.bigquery.BigQuery;

public interface BigQueryService {
  BigQuery get();
  BigQuery get(String projectId, String impersonatedServiceAccount);
  ValidationResult canAccessDataset(BigQuery bigQuery, String projectId, String datasetId);
}
