package io.harness.ccm.billing.bigquery;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.ValidationResult;

import com.google.cloud.bigquery.BigQuery;
@TargetModule(Module._490_CE_COMMONS)
public interface BigQueryService {
  BigQuery get();
  BigQuery get(String projectId, String impersonatedServiceAccount);
  ValidationResult canAccessDataset(BigQuery bigQuery, String projectId, String datasetId);
}
