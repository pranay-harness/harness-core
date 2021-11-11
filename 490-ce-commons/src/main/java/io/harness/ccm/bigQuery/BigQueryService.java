package io.harness.ccm.bigQuery;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.TableResult;
import com.sun.istack.internal.NotNull;

@OwnedBy(CE)
public interface BigQueryService {
  TableResult query(@NotNull String query);

  Job create(@NotNull String query);

  BigQuery get();
}
