package io.harness.connector.validator;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ceazure.BillingExportSpecDTO;
import io.harness.delegate.beans.connector.ceazure.CEAzureConnectorDTO;
import io.harness.delegate.task.TaskParameters;
import io.harness.ng.core.dto.ErrorDetail;
import io.harness.remote.CEAzureSetupConfig;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.authorization.models.RoleAssignment;
import com.azure.resourcemanager.authorization.models.RoleDefinition;
import com.azure.resourcemanager.authorization.models.ServicePrincipal;
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
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Singleton
@OwnedBy(CE)
public class CEAzureConnectorValidator extends AbstractConnectorValidator {
  private static final String AZURE_STORAGE_SUFFIX = "blob.core.windows.net";
  private static final String AZURE_STORAGE_URL_FORMAT = "https://%s.%s";
  private static final String GENERIC_LOGGING_ERROR =
      "Failed to validate accountIdentifier:{} orgIdentifier:{} projectIdentifier:{}";
  private static final String AZURE_ROLE_BILLING = "Storage Blob Data Reader";
  private static final String AZURE_ROLE_OPTIMIZATION = "Contributor";

  @Inject private CEAzureSetupConfig ceAzureSetupConfig;

  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO connectorDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    final CEAzureConnectorDTO ceAzureConnectorDTO = (CEAzureConnectorDTO) connectorDTO;
    final List<CEFeatures> featuresEnabled = ceAzureConnectorDTO.getFeaturesEnabled();
    final String tenantId = ceAzureConnectorDTO.getTenantId();
    final String subscriptionId = ceAzureConnectorDTO.getSubscriptionId();
    HashMap<String, String> rolesAssigned;

    try {
      ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
                                                          .clientId(ceAzureSetupConfig.getAzureAppClientId())
                                                          .clientSecret(ceAzureSetupConfig.getAzureAppClientSecret())
                                                          .tenantId(tenantId)
                                                          .build();
      rolesAssigned = validateAppRoles(tenantId, subscriptionId, clientSecretCredential);

      if (featuresEnabled.contains(CEFeatures.BILLING)) {
        final BillingExportSpecDTO billingExportSpec = ceAzureConnectorDTO.getBillingExportSpec();
        final String storageAccountName = billingExportSpec.getStorageAccountName();
        final String containerName = billingExportSpec.getContainerName();
        final String directoryName = billingExportSpec.getDirectoryName();
        String endpoint = String.format(AZURE_STORAGE_URL_FORMAT, storageAccountName, AZURE_STORAGE_SUFFIX);
        BlobContainerClient blobContainerClient =
            getBlobContainerClient(endpoint, containerName, tenantId, clientSecretCredential);
        validateIfContainerIsPresent(blobContainerClient, directoryName);
        if (!rolesAssigned.containsKey(AZURE_ROLE_BILLING)) {
          return ConnectorValidationResult.builder()
              .status(ConnectivityStatus.FAILURE)
              .errors(
                  ImmutableList.of(ErrorDetail.builder()
                                       .message("Missing role " + AZURE_ROLE_BILLING + " assigned to storage account")
                                       .build()))
              .errorSummary(AZURE_ROLE_BILLING + " role assignment check failed")
              .testedAt(Instant.now().toEpochMilli())
              .build();
        }
      }
      if (featuresEnabled.contains(CEFeatures.OPTIMIZATION)) {
        if (!rolesAssigned.containsKey(AZURE_ROLE_OPTIMIZATION)
            && !rolesAssigned.get(AZURE_ROLE_OPTIMIZATION).equals("/subscriptions/" + subscriptionId)) {
          return ConnectorValidationResult.builder()
              .status(ConnectivityStatus.FAILURE)
              .errors(ImmutableList.of(ErrorDetail.builder()
                                           .message("Missing role " + AZURE_ROLE_OPTIMIZATION
                                               + " assigned to scope /subscriptions/" + subscriptionId)
                                           .build()))
              .errorSummary(AZURE_ROLE_OPTIMIZATION + " role assignment check failed")
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
      }
      log.error(GENERIC_LOGGING_ERROR, accountIdentifier, orgIdentifier, projectIdentifier, ex);
      return ConnectorValidationResult.builder()
          .status(ConnectivityStatus.FAILURE)
          .errorSummary("Exception while validating billing export details")
          .testedAt(Instant.now().toEpochMilli())
          .build();
    }
    log.info("Validation successful");
    return ConnectorValidationResult.builder()
        .status(ConnectivityStatus.SUCCESS)
        .testedAt(Instant.now().toEpochMilli())
        .build();
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

  public void validateIfContainerIsPresent(BlobContainerClient blobContainerClient, String directoryName)
      throws Exception {
    // List the blob(s) in the container.
    for (BlobItem blobItem : blobContainerClient.listBlobsByHierarchy(directoryName)) {
      return;
    }
    throw new Exception("The specified directory does not exist");
  }

  @VisibleForTesting
  public BlobContainerClient getBlobContainerClient(
      String endpoint, String containerName, String tenantId, ClientSecretCredential clientSecretCredential) {
    BlobServiceClient blobServiceClient =
        new BlobServiceClientBuilder().endpoint(endpoint).credential(clientSecretCredential).buildClient();
    return blobServiceClient.getBlobContainerClient(containerName);
  }

  @VisibleForTesting
  public HashMap<String, String> validateAppRoles(
      String tenantId, String subscriptionId, ClientSecretCredential clientSecretCredential) {
    HashMap<String, String> rolesAssigned = new HashMap<>();
    AzureProfile profile = new AzureProfile(tenantId, subscriptionId, AzureEnvironment.AZURE);
    AzureResourceManager azureResourceManager = AzureResourceManager.configure()
                                                    .withLogLevel(HttpLogDetailLevel.BASIC)
                                                    .authenticate(clientSecretCredential, profile)
                                                    .withSubscription(subscriptionId);
    ServicePrincipal servicePrincipal =
        azureResourceManager.accessManagement().servicePrincipals().getByName(ceAzureSetupConfig.getAzureAppClientId());
    PagedIterable<RoleAssignment> roles =
        azureResourceManager.accessManagement().roleAssignments().listByServicePrincipal(servicePrincipal);

    for (RoleAssignment item : roles) {
      RoleDefinition role = azureResourceManager.accessManagement().roleDefinitions().getById(item.roleDefinitionId());
      rolesAssigned.put(role.roleName(), item.scope());
      log.info("role: %s, scope: %s, name: %s, id: %s\n", role.roleName(), item.scope(), item.name(), item.id());
    }
    return rolesAssigned;
  }
}
