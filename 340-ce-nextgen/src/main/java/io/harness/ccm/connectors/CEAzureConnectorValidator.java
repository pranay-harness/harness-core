package io.harness.ccm.connectors;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.ceazure.BillingExportSpecDTO;
import io.harness.delegate.beans.connector.ceazure.CEAzureConnectorDTO;
import io.harness.ng.core.dto.ErrorDetail;
import io.harness.remote.CEAzureSetupConfig;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobStorageException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.microsoft.aad.msal4j.MsalServiceException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CE)
public class CEAzureConnectorValidator {
  private static final String AZURE_STORAGE_SUFFIX = "blob.core.windows.net";
  private static final String AZURE_STORAGE_URL_FORMAT = "https://%s.%s";
  private static final String GENERIC_LOGGING_ERROR =
      "Failed to validate accountIdentifier:{} orgIdentifier:{} projectIdentifier:{}";

  @Inject private static CEAzureSetupConfig ceAzureSetupConfig;

  public static ConnectorValidationResult validate(
      ConnectorResponseDTO connectorResponseDTO, String accountIdentifier) {
    final CEAzureConnectorDTO ceAzureConnectorDTO =
        (CEAzureConnectorDTO) connectorResponseDTO.getConnector().getConnectorConfig();
    final List<CEFeatures> featuresEnabled = ceAzureConnectorDTO.getFeaturesEnabled();
    String projectIdentifier = connectorResponseDTO.getConnector().getProjectIdentifier();
    String orgIdentifier = connectorResponseDTO.getConnector().getOrgIdentifier();
    String connectorIdentifier = connectorResponseDTO.getConnector().getIdentifier();

    try {
      if (featuresEnabled.contains(CEFeatures.BILLING)) {
        final BillingExportSpecDTO billingExportSpec = ceAzureConnectorDTO.getBillingExportSpec();
        final String storageAccountName = billingExportSpec.getStorageAccountName();
        final String containerName = billingExportSpec.getContainerName();
        final String directoryName = billingExportSpec.getDirectoryName();
        final String tenantId = ceAzureConnectorDTO.getTenantId();
        String endpoint = String.format(AZURE_STORAGE_URL_FORMAT, storageAccountName, AZURE_STORAGE_SUFFIX);
        BlobContainerClient blobContainerClient = getBlobContainerClient(endpoint, containerName, tenantId);
        validateIfContainerIsPresent(blobContainerClient, directoryName);
      }
    } catch (BlobStorageException ex) {
      if (ex.getErrorCode().toString().equals("ContainerNotFound")) {
        return ConnectorValidationResult.builder()
            .status(ConnectivityStatus.FAILURE)
            .errors(ImmutableList.of(ErrorDetail.builder()
                                         .code(ex.getStatusCode())
                                         .reason(ex.getErrorCode().toString())
                                         .message("Exception while validating storage account details")
                                         .build()))
            .errorSummary("The specified container does not exist")
            .testedAt(Instant.now().toEpochMilli())
            .build();
      } else if (ex.getErrorCode().toString().equals("AuthorizationPermissionMismatch")) {
        return ConnectorValidationResult.builder()
            .status(ConnectivityStatus.FAILURE)
            .errors(ImmutableList.of(ErrorDetail.builder()
                                         .code(ex.getStatusCode())
                                         .reason(ex.getErrorCode().toString())
                                         .message("Exception while validating storage account details")
                                         .build()))
            .errorSummary("Authorization permission mismatch")
            .testedAt(Instant.now().toEpochMilli())
            .build();
      }
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.FAILURE)
          .errors(ImmutableList.of(ErrorDetail.builder()
                                       .code(ex.getStatusCode())
                                       .reason(ex.getErrorCode().toString())
                                       .message("Exception while validating storage account details")
                                       .build()))
          .errorSummary(ex.getErrorCode().toString())
          .testedAt(Instant.now().toEpochMilli())
          .build();
    } catch (MsalServiceException ex) {
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.FAILURE)
          .errors(ImmutableList.of(ErrorDetail.builder()
                                       .code(ex.statusCode())
                                       .reason(ex.errorCode())
                                       .message("The specified tenantId does not exist")
                                       .build()))
          .errorSummary("Exception while validating tenantId")
          .testedAt(Instant.now().toEpochMilli())
          .build();
    } catch (Exception ex) {
      if (ex.getCause() instanceof UnknownHostException) {
        return ConnectorValidationResult.builder()
            .status(ConnectivityStatus.FAILURE)
            .errorSummary("The specified storage account does not exist")
            .testedAt(Instant.now().toEpochMilli())
            .build();
      }
      log.error(GENERIC_LOGGING_ERROR, accountIdentifier, orgIdentifier, projectIdentifier, ex);
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.FAILURE)
          .errorSummary("Exception while validating billing export details")
          .testedAt(Instant.now().toEpochMilli())
          .build();
    }
    return ConnectorValidationResult.builder()
        .status(ConnectivityStatus.SUCCESS)
        .testedAt(Instant.now().toEpochMilli())
        .build();
  }

  public static void validateIfContainerIsPresent(BlobContainerClient blobContainerClient, String directoryName)
      throws Exception {
    // List the blob(s) in the container.
    for (BlobItem blobItem : blobContainerClient.listBlobsByHierarchy(directoryName)) {
      return;
    }
    throw new Exception("The specified directory does not exist");
  }

  @VisibleForTesting
  public static BlobContainerClient getBlobContainerClient(String endpoint, String containerName, String tenantId) {
    ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                                                        .clientId(ceAzureSetupConfig.getAzureAppClientId())
                                                        .clientSecret(ceAzureSetupConfig.getAzureAppClientSecret())
                                                        .tenantId(tenantId)
                                                        .build();
    BlobServiceClient blobServiceClient =
        new BlobServiceClientBuilder().endpoint(endpoint).credential(clientSecretCredential).buildClient();
    return blobServiceClient.getBlobContainerClient(containerName);
  }
}
