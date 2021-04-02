package io.harness.cdng.k8s.resources.gcp.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.common.beans.SetupAbstractionKeys;
import io.harness.cdng.k8s.resources.gcp.GcpResponseDTO;
import io.harness.cdng.k8s.resources.gcp.service.GcpResourceService;
import io.harness.common.NGTaskType;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.task.gcp.GcpTaskType;
import io.harness.delegate.task.gcp.request.GcpListClustersRequest;
import io.harness.delegate.task.gcp.request.GcpRequest;
import io.harness.delegate.task.gcp.request.GcpTaskParameters;
import io.harness.delegate.task.gcp.response.GcpClusterListTaskResponse;
import io.harness.exception.GcpServerException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Singleton
@OwnedBy(CDP)
public class GcpResourceServiceImpl implements GcpResourceService {
  private final ConnectorService connectorService;
  private final SecretManagerClientService secretManagerClientService;
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @VisibleForTesting static final int timeoutInSecs = 30;
  private static final String ERROR_MESSAGE = "GCP cluster list task failure due to error";

  @Inject
  public GcpResourceServiceImpl(@Named(DEFAULT_CONNECTOR_SERVICE) ConnectorService connectorService,
      SecretManagerClientService secretManagerClientService) {
    this.connectorService = connectorService;
    this.secretManagerClientService = secretManagerClientService;
  }

  @Override
  public GcpResponseDTO getClusterNames(
      IdentifierRef gcpConnectorRef, String accountId, String orgIdentifier, String projectIdentifier) {
    GcpConnectorDTO connector = getConnector(gcpConnectorRef);
    BaseNGAccess baseNGAccess = BaseNGAccess.builder()
                                    .accountIdentifier(gcpConnectorRef.getAccountIdentifier())
                                    .orgIdentifier(orgIdentifier)
                                    .projectIdentifier(projectIdentifier)
                                    .build();

    List<EncryptedDataDetail> encryptionDetails = getEncryptionDetails(connector, baseNGAccess);
    GcpListClustersRequest request = GcpListClustersRequest.builder()
                                         .gcpManualDetailsDTO(gcpManualDetailsDTO(connector))
                                         .delegateSelectors(connector.getDelegateSelectors())
                                         .encryptionDetails(encryptionDetails)
                                         .build();

    GcpClusterListTaskResponse gcpClusterListTaskResponse =
        executeSyncTask(request, GcpTaskType.LIST_CLUSTERS, baseNGAccess);
    return GcpResponseDTO.builder().clusterNames(gcpClusterListTaskResponse.getClusterNames()).build();
  }

  private GcpClusterListTaskResponse executeSyncTask(
      GcpRequest gcpRequest, GcpTaskType taskType, BaseNGAccess ngAccess) {
    DelegateResponseData responseData = getResponseData(ngAccess, gcpRequest, taskType);
    return getTaskExecutionResponse(responseData);
  }

  private DelegateResponseData getResponseData(BaseNGAccess ngAccess, GcpRequest gcpRequest, GcpTaskType gcpTaskType) {
    GcpTaskParameters gcpTaskParameters = GcpTaskParameters.builder()
                                              .accountId(ngAccess.getAccountIdentifier())
                                              .gcpTaskType(gcpTaskType)
                                              .gcpRequest(gcpRequest)
                                              .build();
    final DelegateTaskRequest delegateTaskRequest =
        DelegateTaskRequest.builder()
            .accountId(ngAccess.getAccountIdentifier())
            .taskType(NGTaskType.GCP_TASK.name())
            .taskParameters(gcpTaskParameters)
            .executionTimeout(java.time.Duration.ofSeconds(timeoutInSecs))
            .taskSetupAbstraction(SetupAbstractionKeys.orgIdentifier, ngAccess.getOrgIdentifier())
            .taskSetupAbstraction(SetupAbstractionKeys.projectIdentifier, ngAccess.getProjectIdentifier())
            .build();
    return delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
  }

  private GcpClusterListTaskResponse getTaskExecutionResponse(DelegateResponseData responseData) {
    if (responseData instanceof ErrorNotifyResponseData) {
      ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) responseData;
      throw new GcpServerException(ERROR_MESSAGE + " - " + errorNotifyResponseData.getErrorMessage());
    }

    GcpClusterListTaskResponse gcpTaskResponse = (GcpClusterListTaskResponse) responseData;
    if (gcpTaskResponse.getCommandExecutionStatus() != SUCCESS) {
      throw new GcpServerException(ERROR_MESSAGE + " - " + gcpTaskResponse.getErrorMessage()
          + " with error detail: " + gcpTaskResponse.getErrorDetail());
    }
    return gcpTaskResponse;
  }

  private GcpConnectorDTO getConnector(IdentifierRef gcpConnectorRef) {
    Optional<ConnectorResponseDTO> connectorDTO = connectorService.get(gcpConnectorRef.getAccountIdentifier(),
        gcpConnectorRef.getOrgIdentifier(), gcpConnectorRef.getProjectIdentifier(), gcpConnectorRef.getIdentifier());

    if (!connectorDTO.isPresent() || !isGcpConnector(connectorDTO.get())) {
      throw new InvalidRequestException(String.format("GCP Connector not found for identifier : [%s] with scope: [%s]",
                                            gcpConnectorRef.getIdentifier(), gcpConnectorRef.getScope()),
          WingsException.USER);
    }
    ConnectorInfoDTO connectors = connectorDTO.get().getConnector();
    return (GcpConnectorDTO) connectors.getConnectorConfig();
  }

  private boolean isGcpConnector(@Valid @NotNull ConnectorResponseDTO connectorResponseDTO) {
    return ConnectorType.GCP == (connectorResponseDTO.getConnector().getConnectorType());
  }

  private List<EncryptedDataDetail> getEncryptionDetails(
      @Nonnull GcpConnectorDTO gcpConnectorDTO, @Nonnull NGAccess ngAccess) {
    List<DecryptableEntity> gcpDecryptableEntities = gcpConnectorDTO.getDecryptableEntities();
    if (isNotEmpty(gcpDecryptableEntities)) {
      return secretManagerClientService.getEncryptionDetails(ngAccess, gcpDecryptableEntities.get(0));
    }

    return Collections.emptyList();
  }

  private GcpManualDetailsDTO gcpManualDetailsDTO(GcpConnectorDTO gcpConnectorDTO) {
    if (gcpConnectorDTO.getCredential().getGcpCredentialType() == GcpCredentialType.MANUAL_CREDENTIALS) {
      return (GcpManualDetailsDTO) gcpConnectorDTO.getCredential().getConfig();
    }

    return null;
  }
}
