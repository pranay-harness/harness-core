package io.harness.ccm.billing.bigquery;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.ccm.billing.GcpServiceAccountServiceImpl.CE_GCP_CREDENTIALS_PATH;
import static io.harness.ccm.billing.GcpServiceAccountServiceImpl.getCredentials;
import static io.harness.ccm.billing.GcpServiceAccountServiceImpl.getImpersonatedCredentials;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.ValidationResult;

import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.inject.Singleton;
import com.sun.istack.internal.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(CE)
public class BigQueryServiceImpl implements BigQueryService, io.harness.ccm.bigQuery.BigQueryService {
  @Override
  public BigQuery get() {
    return get(null, null);
  }

  @Override
  public TableResult query(@NotNull String query) {
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();

    TableResult result = null;
    try {
      result = this.get().query(queryConfig);
    } catch (InterruptedException e) {
      log.error("Failed to execute: {}", query, e);
      Thread.currentThread().interrupt();
      return null;
    }

    return result;
  }

  @Override
  public Job create(@NotNull String query) {
    QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(query).setDryRun(true).setUseQueryCache(false).build();

    try {
      return this.get().create(JobInfo.of(queryConfig));
    } catch (BigQueryException e) {
      log.error("Failed to create bigQuery: {}", query, e);
      throw new InvalidRequestException(e.getMessage());
    }
  }

  @Override
  public BigQuery get(String projectId, String impersonatedServiceAccount) {
    ServiceAccountCredentials sourceCredentials = getCredentials(CE_GCP_CREDENTIALS_PATH);
    Credentials credentials = getImpersonatedCredentials(sourceCredentials, impersonatedServiceAccount);

    BigQueryOptions.Builder bigQueryOptionsBuilder = BigQueryOptions.newBuilder().setCredentials(credentials);

    if (projectId != null) {
      bigQueryOptionsBuilder.setProjectId(projectId);
    }
    return bigQueryOptionsBuilder.build().getService();
  }

  @Override
  public ValidationResult canAccessDataset(BigQuery bigQuery, String projectId, String datasetId) {
    try {
      Dataset dataset = bigQuery.getDataset(datasetId);
      if (dataset == null) {
        return ValidationResult.builder()
            .valid(false)
            .errorMessage(format("Unable to find the dataset \"%s\".", datasetId))
            .build();
      } else {
        return ValidationResult.builder().valid(true).build();
      }
    } catch (BigQueryException be) {
      log.error("Unable to access BigQuery Dataset", be);
      return ValidationResult.builder().valid(false).errorMessage(be.getMessage()).build();
    }
  }
}
