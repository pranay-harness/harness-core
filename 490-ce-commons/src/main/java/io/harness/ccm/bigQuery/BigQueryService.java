package io.harness.ccm.bigQuery;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;

@OwnedBy(CE)
public interface BigQueryService {
  BigQuery get();
  ServiceAccountCredentials getCredentials(String googleCredentialPathSystemEnv);
}
