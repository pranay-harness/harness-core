package io.harness.ccm.bigQuery;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.exception.InvalidRequestException;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.inject.Singleton;
import com.sun.istack.internal.NotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class BigQueryServiceImpl implements BigQueryService {
  public static final String GCP_CREDENTIALS_PATH = "GOOGLE_CREDENTIALS_PATH";

  @SneakyThrows
  @Override
  public TableResult query(@NotNull String query) {
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();

    try {
      return this.get().query(queryConfig);
    } catch (InterruptedException e) {
      log.error("Failed to execute query: {}", query, e);
      throw e;
    }
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
  public BigQuery get() {
    ServiceAccountCredentials credentials = getCredentials(GCP_CREDENTIALS_PATH);
    BigQueryOptions.Builder bigQueryOptionsBuilder = BigQueryOptions.newBuilder().setCredentials(credentials);

    return bigQueryOptionsBuilder.build().getService();
  }

  public ServiceAccountCredentials getCredentials(String googleCredentialPathSystemEnv) {
    String googleCredentialsPath = System.getenv(googleCredentialPathSystemEnv);
    if (isEmpty(googleCredentialsPath)) {
      log.error("Missing environment variable for GCP credentials.");
    }
    File credentialsFile = new File(googleCredentialsPath);
    ServiceAccountCredentials credentials = null;
    try (FileInputStream serviceAccountStream = new FileInputStream(credentialsFile)) {
      credentials = ServiceAccountCredentials.fromStream(serviceAccountStream);
    } catch (FileNotFoundException e) {
      log.error("Failed to find Google credential file for the CE service account in the specified path.", e);
    } catch (IOException e) {
      log.error("Failed to get Google credential file for the CE service account.", e);
    }
    return credentials;
  }
}
