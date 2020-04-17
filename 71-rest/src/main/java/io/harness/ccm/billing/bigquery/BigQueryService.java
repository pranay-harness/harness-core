package io.harness.ccm.billing.bigquery;

import com.google.cloud.bigquery.BigQuery;

public interface BigQueryService {
  BigQuery get();
  BigQuery get(String projectId, String impersonatedServiceAccount);
}
