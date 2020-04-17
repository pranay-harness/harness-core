package io.harness.ccm.billing.bigquery;

import com.google.cloud.bigquery.BigQuery;

import lombok.experimental.UtilityClass;

@UtilityClass
public class BigQueryUtils {
  boolean canAccessDataset(BigQuery bigQuery, String datasetId) {
    BigQuery.DatasetOption datasetOption;
    bigQuery.getDataset(datasetId);
    return true;
  }
}
