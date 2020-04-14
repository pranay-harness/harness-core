package io.harness.ccm.billing.bigquery;

import static com.hazelcast.util.Preconditions.checkFalse;
import static com.hazelcast.util.Preconditions.checkTrue;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@Slf4j
@Singleton
public class BigQueryServiceImpl implements BigQueryService {
  private static final String CE_BIGQUERY_GOOGLE_APPLICATION_CREDENTIALS_PATH =
      "CE_BIGQUERY_GOOGLE_APPLICATION_CREDENTIALS";

  @Override
  public BigQuery get() {
    // read the credential path from env variables
    String googleCredentialsPath = System.getenv(CE_BIGQUERY_GOOGLE_APPLICATION_CREDENTIALS_PATH);
    checkFalse(isEmpty(googleCredentialsPath), "Missing environment variable for GCP credentials.");
    File credentialsFile = new File(googleCredentialsPath);
    try (FileInputStream serviceAccountStream = new FileInputStream(credentialsFile)) {
      ServiceAccountCredentials.fromStream(serviceAccountStream);
    } catch (FileNotFoundException e) {
      logger.error("Failed to fine Google credential file for BigQuery in the specified path.", e);
    } catch (IOException e) {
      logger.error("Failed to get Google credential file for BigQuery.", e);
    }

    BigQuery bigQuery = BigQueryOptions.newBuilder().build().getService();
    checkTrue(verifyAuth(bigQuery), "Failed to authenticate with the BigQuery credentials.");
    return bigQuery;
  }

  boolean verifyAuth(BigQuery bigQuery) {
    BigQuery.DatasetListOption listOption = BigQuery.DatasetListOption.all();
    bigQuery.listDatasets(listOption);
    return true;
  }
}
