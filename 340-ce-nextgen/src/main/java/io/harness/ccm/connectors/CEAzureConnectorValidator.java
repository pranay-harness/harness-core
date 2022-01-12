/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.connectors;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.CENextGenConfiguration;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ceazure.BillingExportSpecDTO;
import io.harness.delegate.beans.connector.ceazure.CEAzureConnectorDTO;
import io.harness.ng.core.dto.ErrorDetail;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.microsoft.aad.msal4j.MsalServiceException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Singleton
@OwnedBy(HarnessTeam.CE)
public class CEAzureConnectorValidator extends io.harness.ccm.connectors.AbstractCEConnectorValidator {
  private final String AZURE_STORAGE_SUFFIX = "blob.core.windows.net";
  private final String AZURE_STORAGE_URL_FORMAT = "https://%s.%s";
  private final String GENERIC_LOGGING_ERROR =
      "Failed to validate accountIdentifier:{} orgIdentifier:{} projectIdentifier:{} connectorIdentifier:{} ";

  @Inject CENextGenConfiguration configuration;
  @Inject CEConnectorsHelper ceConnectorsHelper;

  public ConnectorValidationResult validate(ConnectorResponseDTO connectorResponseDTO, String accountIdentifier) {
    final CEAzureConnectorDTO ceAzureConnectorDTO =
        (CEAzureConnectorDTO) connectorResponseDTO.getConnector().getConnectorConfig();
    final List<CEFeatures> featuresEnabled = ceAzureConnectorDTO.getFeaturesEnabled();
    String projectIdentifier = connectorResponseDTO.getConnector().getProjectIdentifier();
    String orgIdentifier = connectorResponseDTO.getConnector().getOrgIdentifier();
    String connectorIdentifier = connectorResponseDTO.getConnector().getIdentifier();
    long oneDayOld = Instant.now().toEpochMilli() - 24 * 60 * 60 * 1000;

    BillingExportSpecDTO billingExportSpec = ceAzureConnectorDTO.getBillingExportSpec();
    if (billingExportSpec == null) {
      log.error("No billing export spec found for this connector {}", connectorIdentifier);
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.FAILURE)
          .errorSummary("Invalid connector configuration")
          .testedAt(Instant.now().toEpochMilli())
          .build();
    }

    String storageAccountName = billingExportSpec.getStorageAccountName();
    String containerName = billingExportSpec.getContainerName();
    String directoryName = billingExportSpec.getDirectoryName();
    String reportName = billingExportSpec.getReportName();
    String tenantId = ceAzureConnectorDTO.getTenantId();
    String prefix = directoryName + "/" + reportName + "/" + ceConnectorsHelper.getReportMonth();
    final List<ErrorDetail> errorList = new ArrayList<>();

    try {
      if (featuresEnabled.contains(CEFeatures.BILLING)) {
        String endpoint = String.format(AZURE_STORAGE_URL_FORMAT, storageAccountName, AZURE_STORAGE_SUFFIX);
        BlobContainerClient blobContainerClient = getBlobContainerClient(endpoint, containerName, tenantId);
        errorList.addAll(validateIfFileIsPresent(getBlobItems(blobContainerClient, prefix), prefix));
        if (!errorList.isEmpty()) {
          return ConnectorValidationResult.builder()
              .status(ConnectivityStatus.FAILURE)
              .errors(errorList)
              .errorSummary("No billing export file is found")
              .testedAt(Instant.now().toEpochMilli())
              .build();
        }
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
      } else if (ex.getMessage() != null && ex.getMessage().startsWith("No billing export file")) {
        return ConnectorValidationResult.builder()
            .status(ConnectivityStatus.FAILURE)
            .errorSummary(ex.getMessage())
            .testedAt(Instant.now().toEpochMilli())
            .build();
      } else {
        log.error(GENERIC_LOGGING_ERROR, accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier, ex);
        return ConnectorValidationResult.builder()
            .status(ConnectivityStatus.FAILURE)
            .errorSummary("Exception while validating billing export details")
            .testedAt(Instant.now().toEpochMilli())
            .build();
      }
    }

    // Check for data at destination only when 24 hrs have elapsed since connector last modified at
    if (connectorResponseDTO.getLastModifiedAt() < oneDayOld) {
      if (featuresEnabled.contains(CEFeatures.BILLING)
          && !ceConnectorsHelper.isDataSyncCheck(accountIdentifier, connectorIdentifier, ConnectorType.CE_AZURE,
              ceConnectorsHelper.JOB_TYPE_CLOUDFUNCTION)) {
        // Issue with CFs
        return ConnectorValidationResult.builder()
            .errorSummary("Error with processing data. Please contact Harness support")
            .status(ConnectivityStatus.FAILURE)
            .build();
      }
    }
    log.info("Validation successful for connector {}", connectorIdentifier);
    return ConnectorValidationResult.builder()
        .status(ConnectivityStatus.SUCCESS)
        .testedAt(Instant.now().toEpochMilli())
        .build();
  }

  public Collection<ErrorDetail> validateIfFileIsPresent(
      com.azure.core.http.rest.PagedIterable<BlobItem> blobItems, String prefix) {
    List<ErrorDetail> errorDetails = new ArrayList<>();
    String latestFileName = "";
    Instant latestFileLastModifiedTime = Instant.EPOCH;
    Instant lastModifiedTime;
    // Caveat: This can be slow for some accounts.
    for (BlobItem blobItem : blobItems) {
      lastModifiedTime = Instant.from(blobItem.getProperties().getLastModified());
      if (blobItem.getName().endsWith(".csv")) {
        if (lastModifiedTime.compareTo(latestFileLastModifiedTime) > 0) {
          latestFileLastModifiedTime = lastModifiedTime;
          latestFileName = blobItem.getName();
        }
      }
    }
    log.info("Latest .csv.gz file in {} latestFileName: {} latestFileLastModifiedTime: {}", prefix, latestFileName,
        latestFileLastModifiedTime);
    if (!latestFileName.isEmpty()
        && latestFileLastModifiedTime.getEpochSecond() < (Instant.now().getEpochSecond() - 24 * 60 * 60)) {
      errorDetails.add(
          ErrorDetail.builder()
              .reason(String.format("No billing export file is found in last 24 hrs in %s. "
                      + "Please verify your billing export config in your Azure account and CCM connector. "
                      + "Follow CCM documentation for more information",
                  prefix))
              .message("No billing export file is found")
              .code(403)
              .build());
    }
    return errorDetails;
  }

  @VisibleForTesting
  public BlobContainerClient getBlobContainerClient(String endpoint, String containerName, String tenantId) {
    ClientSecretCredential clientSecretCredential =
        new ClientSecretCredentialBuilder()
            .clientId(configuration.getCeAzureSetupConfig().getAzureAppClientId())
            .clientSecret(configuration.getCeAzureSetupConfig().getAzureAppClientSecret())
            .tenantId(tenantId)
            .build();
    BlobServiceClient blobServiceClient =
        new BlobServiceClientBuilder().endpoint(endpoint).credential(clientSecretCredential).buildClient();
    return blobServiceClient.getBlobContainerClient(containerName);
  }

  public com.azure.core.http.rest.PagedIterable<BlobItem> getBlobItems(
      BlobContainerClient blobContainerClient, String prefix) {
    return blobContainerClient.listBlobs(new ListBlobsOptions().setPrefix(prefix), null);
  }
}
