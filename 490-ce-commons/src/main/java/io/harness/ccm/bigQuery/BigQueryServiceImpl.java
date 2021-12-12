package io.harness.ccm.bigQuery;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Timestamp;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class BigQueryServiceImpl implements BigQueryService {
  public static final String GCP_CREDENTIALS_PATH = "GOOGLE_CREDENTIALS_PATH";
  public static String BQ_UPDATE_DATA_SYNC_STATUS_QUERY = "INSERT INTO `{}.CE_INTERNAL.connectorDataSyncStatus`"
      + "(accountId, connectorId, lastSuccessfullExecutionAt, jobType, cloudProviderId)"
      + "VALUES ('{}', '{}', '{}', 'batch', '{}')";

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

  public boolean updateDataSyncStatusTable(
      String gcpProjectId, String accountId, String connectorId, String cloudProviderId) {
    Long date = System.currentTimeMillis();
    Timestamp timestamp = new Timestamp(date);
    String query = String.format(
        BQ_UPDATE_DATA_SYNC_STATUS_QUERY, gcpProjectId, accountId, connectorId, timestamp.toString(), cloudProviderId);
    log.info("Query: {}", query);
    return true;
  }
}
