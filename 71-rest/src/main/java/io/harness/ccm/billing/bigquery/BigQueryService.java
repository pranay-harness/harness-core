package io.harness.ccm.billing.bigquery;

import com.google.cloud.bigquery.BigQuery;

import software.wings.beans.ValidationResult;

public interface BigQueryService {
  BigQuery get();
  BigQuery get(String projectId, String impersonatedServiceAccount);
  ValidationResult canAccessDataset(BigQuery bigQuery, String projectId, String datasetId);
}
