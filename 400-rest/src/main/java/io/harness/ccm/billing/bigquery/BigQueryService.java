package io.harness.ccm.billing.bigquery;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.ValidationResult;

import com.google.cloud.bigquery.BigQuery;

@OwnedBy(CE)
public interface BigQueryService {
  BigQuery get();
  BigQuery get(String projectId, String impersonatedServiceAccount);
  ValidationResult canAccessDataset(BigQuery bigQuery, String projectId, String datasetId);
}
