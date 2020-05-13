package io.harness.ccm.billing.bigquery;

import static io.harness.ccm.GcpServiceAccountService.CE_GCP_CREDENTIALS_PATH;
import static io.harness.ccm.GcpServiceAccountService.getCredentials;
import static java.lang.String.format;

import com.google.auth.Credentials;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Dataset;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ValidationResult;

import java.util.Arrays;

@Slf4j
@Singleton
public class BigQueryServiceImpl implements BigQueryService {
  @Override
  public BigQuery get() {
    return get(null, null);
  }

  @Override
  public BigQuery get(String projectId, String impersonatedServiceAccount) {
    Credentials credentials = null;
    ServiceAccountCredentials sourceCredentials = getCredentials(CE_GCP_CREDENTIALS_PATH);
    ImpersonatedCredentials targetCredentials;
    if (impersonatedServiceAccount == null) {
      credentials = sourceCredentials;
    } else {
      targetCredentials = ImpersonatedCredentials.create(sourceCredentials, impersonatedServiceAccount, null,
          Arrays.asList("https://www.googleapis.com/auth/cloud-platform"), 300);
      credentials = targetCredentials;
    }
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
      logger.error("Unable to access BigQuery Dataset", be);
      return ValidationResult.builder().valid(false).errorMessage(be.getMessage()).build();
    }
  }
}
