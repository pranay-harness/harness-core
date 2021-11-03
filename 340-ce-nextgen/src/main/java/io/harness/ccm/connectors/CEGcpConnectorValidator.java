package io.harness.ccm.connectors;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostConnectorDTO;
import io.harness.ng.core.dto.ErrorDetail;

import com.google.api.gax.paging.Page;
import com.google.auth.Credentials;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.Table;
import com.google.common.collect.ImmutableList;
import com.google.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CE)
public class CEGcpConnectorValidator {
  public static final String GCP_CREDENTIALS_PATH = "CE_GCP_CREDENTIALS_PATH";
  public static final String GCP_BILLING_EXPORT_V_1 = "gcp_billing_export_v1";

  public static ConnectorValidationResult validate(
      ConnectorResponseDTO connectorResponseDTO, String accountIdentifier) {
    final GcpCloudCostConnectorDTO gcpCloudCostConnectorDTO =
        (GcpCloudCostConnectorDTO) connectorResponseDTO.getConnector().getConnectorConfig();
    String projectIdentifier = connectorResponseDTO.getConnector().getProjectIdentifier();
    String orgIdentifier = connectorResponseDTO.getConnector().getOrgIdentifier();
    String connectorIdentifier = connectorResponseDTO.getConnector().getIdentifier();
    String projectId = gcpCloudCostConnectorDTO.getProjectId();
    String datasetId = gcpCloudCostConnectorDTO.getBillingExportSpec().getDatasetId();
    String impersonatedServiceAccount = gcpCloudCostConnectorDTO.getServiceAccountEmail();
    try {
      ConnectorValidationResult connectorValidationResult =
          validateAccessToBillingReport(projectId, datasetId, impersonatedServiceAccount);
      if (connectorValidationResult != null) {
        return connectorValidationResult;
      } else {
        // Check for the presence of data in unifiedTable
      }
    } catch (Exception ex) {
      log.error("Unknown error occurred", ex);
      return ConnectorValidationResult.builder()
          .errorSummary("Unknown error occurred")
          .status(ConnectivityStatus.FAILURE)
          .build();
    }

    return ConnectorValidationResult.builder()
        .status(ConnectivityStatus.SUCCESS)
        .testedAt(Instant.now().toEpochMilli())
        .build();
  }

  public static ConnectorValidationResult validateAccessToBillingReport(
      String projectId, String datasetId, String impersonatedServiceAccount) {
    boolean isTablePresent = false;
    ServiceAccountCredentials sourceCredentials = getGcpCredentials(GCP_CREDENTIALS_PATH);
    Credentials credentials = getGcpImpersonatedCredentials(sourceCredentials, impersonatedServiceAccount);
    BigQuery bigQuery;
    BigQueryOptions.Builder bigQueryOptionsBuilder = BigQueryOptions.newBuilder().setCredentials(credentials);
    log.info(
        "projectId {}, datasetId {}, impersonatedServiceAccount {}", projectId, datasetId, impersonatedServiceAccount);
    if (projectId != null) {
      bigQueryOptionsBuilder.setProjectId(projectId);
    }
    bigQuery = bigQueryOptionsBuilder.build().getService();

    try {
      Dataset dataset = bigQuery.getDataset(datasetId);
      if (dataset == null) {
        log.error("Unable to find the dataset :" + datasetId);
        return ConnectorValidationResult.builder()
            .status(ConnectivityStatus.FAILURE)
            .errorSummary("Unable to find the dataset " + datasetId + " in project " + projectId
                + ". Please check if dataset exists and service account " + impersonatedServiceAccount
                + " has required permissions")
            .testedAt(Instant.now().toEpochMilli())
            .build();
      } else {
        // Check for presence of table "gcp_billing_export_v1_*"
        Page<Table> tableList = dataset.list(BigQuery.TableListOption.pageSize(1000));
        for (Table table : tableList.getValues()) {
          if (table.getTableId().getTable().contains(GCP_BILLING_EXPORT_V_1)) {
            isTablePresent = true;
          }
        }
        if (!isTablePresent) {
          return ConnectorValidationResult.builder()
              .status(ConnectivityStatus.PARTIAL)
              .errorSummary("Billing table " + GCP_BILLING_EXPORT_V_1 + "not yet present in"
                  + " the dataset " + datasetId + " in project " + projectId)
              .testedAt(Instant.now().toEpochMilli())
              .build();
        }
        return null;
      }
    } catch (BigQueryException be) {
      log.error("Unable to access BigQuery Dataset", be);
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.FAILURE)
          .errors(ImmutableList.of(
              ErrorDetail.builder().code(be.getCode()).reason(be.getMessage()).message(be.getMessage()).build()))
          .errorSummary("Unable to access the dataset " + datasetId + " in project " + projectId)
          .testedAt(Instant.now().toEpochMilli())
          .build();
    }
  }

  public static ServiceAccountCredentials getGcpCredentials(String googleCredentialPathSystemEnv) {
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

  public static Credentials getGcpImpersonatedCredentials(
      ServiceAccountCredentials sourceCredentials, String impersonatedServiceAccount) {
    if (impersonatedServiceAccount == null) {
      return sourceCredentials;
    } else {
      return ImpersonatedCredentials.create(sourceCredentials, impersonatedServiceAccount, null,
          Arrays.asList("https://www.googleapis.com/auth/cloud-platform"), 300);
    }
  }
}
