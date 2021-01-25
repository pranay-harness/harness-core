package io.harness.cdng.artifact.resources.gcr.service;

import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;

import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.artifact.resources.gcr.dtos.GcrBuildDetailsDTO;
import io.harness.cdng.artifact.resources.gcr.dtos.GcrRequestDTO;
import io.harness.cdng.artifact.resources.gcr.dtos.GcrResponseDTO;
import io.harness.cdng.artifact.resources.gcr.mappers.GcrResourceMapper;
import io.harness.common.NGTaskType;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.ArtifactTaskType;
import io.harness.delegate.task.artifacts.gcr.GcrArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.gcr.GcrArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.exception.ArtifactServerException;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@Singleton
public class GcrResourceServiceImpl implements GcrResourceService {
  private final ConnectorService connectorService;
  private final SecretManagerClientService secretManagerClientService;
  private final DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @VisibleForTesting static final int timeoutInSecs = 30;

  @Inject
  public GcrResourceServiceImpl(@Named(DEFAULT_CONNECTOR_SERVICE) ConnectorService connectorService,
      SecretManagerClientService secretManagerClientService, DelegateGrpcClientWrapper delegateGrpcClientWrapper) {
    this.connectorService = connectorService;
    this.secretManagerClientService = secretManagerClientService;
    this.delegateGrpcClientWrapper = delegateGrpcClientWrapper;
  }

  @Override
  public GcrResponseDTO getBuildDetails(IdentifierRef gcrConnectorRef, String imagePath, String registryHostname,
      String projectIdentifier, String orgIdentifier) {
    GcpConnectorDTO connector = getConnector(gcrConnectorRef);
    BaseNGAccess baseNGAccess =
        getBaseNGAccess(gcrConnectorRef.getAccountIdentifier(), orgIdentifier, projectIdentifier);
    List<EncryptedDataDetail> encryptionDetails = getEncryptionDetails(connector, baseNGAccess);
    GcrArtifactDelegateRequest gcrRequest = GcrArtifactDelegateRequest.builder()
                                                .gcpConnectorDTO(connector)
                                                .encryptedDataDetails(encryptionDetails)
                                                .imagePath(imagePath)
                                                .sourceType(ArtifactSourceType.GCR)
                                                .registryHostname(registryHostname)
                                                .build();
    ArtifactTaskExecutionResponse artifactTaskExecutionResponse = executeSyncTask(
        gcrRequest, ArtifactTaskType.GET_BUILDS, baseNGAccess, "Gcr Get Builds task failure due to error");
    return getGcrResponseDTO(artifactTaskExecutionResponse);
  }

  @Override
  public GcrBuildDetailsDTO getSuccessfulBuild(IdentifierRef gcrConnectorRef, String imagePath,
      GcrRequestDTO gcrRequestDTO, String orgIdentifier, String projectIdentifier) {
    GcpConnectorDTO connector = getConnector(gcrConnectorRef);
    BaseNGAccess baseNGAccess =
        getBaseNGAccess(gcrConnectorRef.getAccountIdentifier(), orgIdentifier, projectIdentifier);
    List<EncryptedDataDetail> encryptionDetails = getEncryptionDetails(connector, baseNGAccess);
    GcrArtifactDelegateRequest gcrRequest = GcrArtifactDelegateRequest.builder()
                                                .gcpConnectorDTO(connector)
                                                .encryptedDataDetails(encryptionDetails)
                                                .tag(gcrRequestDTO.getTag())
                                                .tagRegex(gcrRequestDTO.getTagRegex())
                                                .imagePath(imagePath)
                                                .registryHostname(gcrRequestDTO.getRegistryHostname())
                                                .sourceType(ArtifactSourceType.GCR)
                                                .build();
    ArtifactTaskExecutionResponse artifactTaskExecutionResponse =
        executeSyncTask(gcrRequest, ArtifactTaskType.GET_LAST_SUCCESSFUL_BUILD, baseNGAccess,
            "Gcr Get last successful build task failure due to error");
    GcrResponseDTO gcrResponseDTO = getGcrResponseDTO(artifactTaskExecutionResponse);
    if (gcrResponseDTO.getBuildDetailsList().size() != 1) {
      throw new ArtifactServerException("Gcr get last successful build task failure.");
    }
    return gcrResponseDTO.getBuildDetailsList().get(0);
  }

  @Override
  public boolean validateArtifactServer(IdentifierRef gcrConnectorRef, String imagePath, String orgIdentifier,
      String projectIdentifier, String registryHostname) {
    GcpConnectorDTO connector = getConnector(gcrConnectorRef);
    BaseNGAccess baseNGAccess =
        getBaseNGAccess(gcrConnectorRef.getAccountIdentifier(), orgIdentifier, projectIdentifier);
    List<EncryptedDataDetail> encryptionDetails = getEncryptionDetails(connector, baseNGAccess);
    GcrArtifactDelegateRequest gcrRequest = GcrArtifactDelegateRequest.builder()
                                                .imagePath(imagePath)
                                                .registryHostname(registryHostname)
                                                .gcpConnectorDTO(connector)
                                                .encryptedDataDetails(encryptionDetails)
                                                .sourceType(ArtifactSourceType.GCR)
                                                .build();
    ArtifactTaskExecutionResponse artifactTaskExecutionResponse =
        executeSyncTask(gcrRequest, ArtifactTaskType.VALIDATE_ARTIFACT_SERVER, baseNGAccess,
            "Gcr validate artifact server task failure due to error");
    return artifactTaskExecutionResponse.isArtifactServerValid();
  }

  @Override
  public boolean validateArtifactSource(String imagePath, IdentifierRef gcrConnectorRef, String registryHostname,
      String orgIdentifier, String projectIdentifier) {
    GcpConnectorDTO connector = getConnector(gcrConnectorRef);
    BaseNGAccess baseNGAccess =
        getBaseNGAccess(gcrConnectorRef.getAccountIdentifier(), orgIdentifier, projectIdentifier);
    List<EncryptedDataDetail> encryptionDetails = getEncryptionDetails(connector, baseNGAccess);
    GcrArtifactDelegateRequest gcrRequest = GcrArtifactDelegateRequest.builder()
                                                .gcpConnectorDTO(connector)
                                                .registryHostname(registryHostname)
                                                .encryptedDataDetails(encryptionDetails)
                                                .imagePath(imagePath)
                                                .sourceType(ArtifactSourceType.GCR)
                                                .build();
    ArtifactTaskExecutionResponse artifactTaskExecutionResponse =
        executeSyncTask(gcrRequest, ArtifactTaskType.VALIDATE_ARTIFACT_SOURCE, baseNGAccess,
            "Gcr validate artifact source task failure due to error");
    return artifactTaskExecutionResponse.isArtifactSourceValid();
  }

  private GcpConnectorDTO getConnector(IdentifierRef gcrConnectorRef) {
    Optional<ConnectorResponseDTO> connectorDTO = connectorService.get(gcrConnectorRef.getAccountIdentifier(),
        gcrConnectorRef.getOrgIdentifier(), gcrConnectorRef.getProjectIdentifier(), gcrConnectorRef.getIdentifier());

    if (!connectorDTO.isPresent() || !isAGcpConnector(connectorDTO.get())) {
      throw new InvalidRequestException(String.format("Connector not found for identifier : [%s] with scope: [%s]",
                                            gcrConnectorRef.getIdentifier(), gcrConnectorRef.getScope()),
          WingsException.USER);
    }
    ConnectorInfoDTO connectors = connectorDTO.get().getConnector();
    return (GcpConnectorDTO) connectors.getConnectorConfig();
  }

  private boolean isAGcpConnector(@Valid @NotNull ConnectorResponseDTO connectorResponseDTO) {
    return ConnectorType.GCP == (connectorResponseDTO.getConnector().getConnectorType());
  }

  private BaseNGAccess getBaseNGAccess(String accountId, String orgIdentifier, String projectIdentifier) {
    return BaseNGAccess.builder()
        .accountIdentifier(accountId)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .build();
  }

  private List<EncryptedDataDetail> getEncryptionDetails(
      @Nonnull GcpConnectorDTO gcpConnectorDTO, @Nonnull NGAccess ngAccess) {
    return secretManagerClientService.getEncryptionDetails(ngAccess, gcpConnectorDTO.getCredential().getConfig());
  }

  private ArtifactTaskExecutionResponse executeSyncTask(
      GcrArtifactDelegateRequest gcrRequest, ArtifactTaskType taskType, BaseNGAccess ngAccess, String ifFailedMessage) {
    DelegateResponseData responseData = getResponseData(ngAccess, gcrRequest, taskType);
    return getTaskExecutionResponse(responseData, ifFailedMessage);
  }

  private DelegateResponseData getResponseData(
      BaseNGAccess ngAccess, GcrArtifactDelegateRequest gcrRequest, ArtifactTaskType artifactTaskType) {
    ArtifactTaskParameters artifactTaskParameters = ArtifactTaskParameters.builder()
                                                        .accountId(ngAccess.getAccountIdentifier())
                                                        .artifactTaskType(artifactTaskType)
                                                        .attributes(gcrRequest)
                                                        .build();
    final DelegateTaskRequest delegateTaskRequest =
        DelegateTaskRequest.builder()
            .accountId(ngAccess.getAccountIdentifier())
            .taskType(NGTaskType.GCR_ARTIFACT_TASK_NG.name())
            .taskParameters(artifactTaskParameters)
            .executionTimeout(java.time.Duration.ofSeconds(timeoutInSecs))
            .taskSetupAbstraction("orgIdentifier", ngAccess.getOrgIdentifier())
            .taskSetupAbstraction("projectIdentifier", ngAccess.getProjectIdentifier())
            .build();
    return delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
  }

  private ArtifactTaskExecutionResponse getTaskExecutionResponse(
      DelegateResponseData responseData, String ifFailedMessage) {
    if (responseData instanceof ErrorNotifyResponseData) {
      ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) responseData;
      throw new ArtifactServerException(ifFailedMessage + " - " + errorNotifyResponseData.getErrorMessage());
    }
    ArtifactTaskResponse artifactTaskResponse = (ArtifactTaskResponse) responseData;
    if (artifactTaskResponse.getCommandExecutionStatus() != SUCCESS) {
      throw new ArtifactServerException(ifFailedMessage + " - " + artifactTaskResponse.getErrorMessage()
          + " with error code: " + artifactTaskResponse.getErrorCode());
    }
    return artifactTaskResponse.getArtifactTaskExecutionResponse();
  }

  private GcrResponseDTO getGcrResponseDTO(ArtifactTaskExecutionResponse artifactTaskExecutionResponse) {
    List<GcrArtifactDelegateResponse> gcrArtifactDelegateResponses =
        artifactTaskExecutionResponse.getArtifactDelegateResponses()
            .stream()
            .map(delegateResponse -> (GcrArtifactDelegateResponse) delegateResponse)
            .collect(Collectors.toList());
    return GcrResourceMapper.toGcrResponse(gcrArtifactDelegateResponses);
  }
}