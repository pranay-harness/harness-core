package io.harness.connector.validator;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostConnectorDTO;
import io.harness.delegate.task.TaskParameters;
import io.harness.ng.core.dto.ErrorDetail;

import com.google.auth.Credentials;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Dataset;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CE)
public class CEGcpConnectorValidator extends AbstractConnectorValidator {
  public static final String GCP_CREDENTIALS_PATH = "CE_GCP_CREDENTIALS_PATH";

  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO connectorDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    final GcpCloudCostConnectorDTO gcpCloudCostConnectorDTO = (GcpCloudCostConnectorDTO) connectorDTO;
    String projectId = gcpCloudCostConnectorDTO.getProjectId();
    String datasetId = gcpCloudCostConnectorDTO.getBillingExportSpec().getDatasetId();
    String impersonatedServiceAccount = gcpCloudCostConnectorDTO.getServiceAccountEmail();
    try {
      return validateAccessToBillingReport(projectId, datasetId, impersonatedServiceAccount);
    } catch (Exception ex) {
      log.error("Unknown error occurred", ex);
      return ConnectorValidationResult.builder()
          .errorSummary("Unknown error occurred")
          .status(ConnectivityStatus.FAILURE)
          .build();
    }
  }

  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return null;
  }

  @Override
  public String getTaskType() {
    return null;
  }

  public ConnectorValidationResult validateAccessToBillingReport(
      String projectId, String datasetId, String impersonatedServiceAccount) {
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
            .errorSummary("Unable to find the dataset :" + datasetId)
            .testedAt(Instant.now().toEpochMilli())
            .build();
      } else {
        return ConnectorValidationResult.builder()
            .status(ConnectivityStatus.SUCCESS)
            .testedAt(Instant.now().toEpochMilli())
            .build();
      }
    } catch (BigQueryException be) {
      log.error("Unable to access BigQuery Dataset", be);
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.FAILURE)
          .errors(ImmutableList.of(
              ErrorDetail.builder().code(be.getCode()).reason(be.getMessage()).message(be.getMessage()).build()))
          .errorSummary("Unable to access dataset :" + datasetId)
          .testedAt(Instant.now().toEpochMilli())
          .build();
    }
  }

  public ServiceAccountCredentials getGcpCredentials(String googleCredentialPathSystemEnv) {
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
