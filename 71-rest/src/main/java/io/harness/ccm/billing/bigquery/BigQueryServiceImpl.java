package io.harness.ccm.billing.bigquery;

import static com.hazelcast.util.Preconditions.checkFalse;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
    // read the credential path from env variables
    String googleCredentialsPath = System.getenv(CE_BIGQUERY_GOOGLE_APPLICATION_CREDENTIALS_PATH);
    checkFalse(isEmpty(googleCredentialsPath), "Missing environment variable for GCP credentials.");
    File credentialsFile = new File(googleCredentialsPath);
    ServiceAccountCredentials sourceCredentials = null;
    ImpersonatedCredentials targetCredentials;
    try (FileInputStream serviceAccountStream = new FileInputStream(credentialsFile)) {
      sourceCredentials = ServiceAccountCredentials.fromStream(serviceAccountStream);
    } catch (FileNotFoundException e) {
      logger.error("Failed to find Google credential file for BigQuery in the specified path.", e);
    } catch (IOException e) {
      logger.error("Failed to get Google credential file for BigQuery.", e);
    }
    BigQueryOptions.Builder bigQueryOptionsBuilder;

    if (impersonatedServiceAccount == null) {
      bigQueryOptionsBuilder = BigQueryOptions.newBuilder().setCredentials(sourceCredentials);
    } else {
      targetCredentials = ImpersonatedCredentials.create(sourceCredentials, impersonatedServiceAccount, null,
          Arrays.asList("https://www.googleapis.com/auth/cloud-platform"), 300);
      bigQueryOptionsBuilder = BigQueryOptions.newBuilder().setCredentials(targetCredentials);
    }

    if (projectId != null) {
      bigQueryOptionsBuilder.setProjectId(projectId);
    }
    return bigQueryOptionsBuilder.build().getService();
  }
}
