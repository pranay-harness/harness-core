package io.harness.ccm.billing.bigquery;

import static io.harness.ccm.GcpServiceAccountService.getCredentials;

import com.google.auth.Credentials;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
@Singleton
public class BigQueryServiceImpl implements BigQueryService {
  private static final String CE_BIGQUERY_GOOGLE_APPLICATION_CREDENTIALS_PATH =
      "CE_BIGQUERY_GOOGLE_APPLICATION_CREDENTIALS";

  @Override
  public BigQuery get() {
    return get(null, null);
  }

  @Override
  public BigQuery get(String projectId, String impersonatedServiceAccount) {
    Credentials credentials = null;
    ServiceAccountCredentials sourceCredentials = getCredentials(CE_BIGQUERY_GOOGLE_APPLICATION_CREDENTIALS_PATH);
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
  public boolean canAccessDataset(BigQuery bigQuery, String projectId, String datasetId) {
    bigQuery.getDataset(datasetId);
    return true;
  }
}
