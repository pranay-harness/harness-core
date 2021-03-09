package io.harness.cdng.artifact.resources.ecr.services;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.artifact.resources.ecr.dtos.EcrBuildDetailsDTO;
import io.harness.cdng.artifact.resources.ecr.dtos.EcrRequestDTO;
import io.harness.cdng.artifact.resources.ecr.dtos.EcrResponseDTO;
import io.harness.cdng.artifact.resources.ecr.mappers.EcrResourceMapper;
import io.harness.common.NGTaskType;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.ArtifactTaskType;
import io.harness.delegate.task.artifacts.ecr.EcrArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.ecr.EcrArtifactDelegateResponse;
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
import static software.wings.service.impl.aws.model.AwsConstants.AWS_DEFAULT_REGION;

import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;

@Singleton
public class EcrResourceServiceImpl implements EcrResourceService{
    private final ConnectorService connectorService;
    private final SecretManagerClientService secretManagerClientService;
    private final DelegateGrpcClientWrapper delegateGrpcClientWrapper;
    @VisibleForTesting
    static final int timeoutInSecs = 30;
    @Inject
    public EcrResourceServiceImpl(@Named(DEFAULT_CONNECTOR_SERVICE) ConnectorService connectorService,
                                  SecretManagerClientService secretManagerClientService, DelegateGrpcClientWrapper delegateGrpcClientWrapper) {
        this.connectorService = connectorService;
        this.secretManagerClientService = secretManagerClientService;
        this.delegateGrpcClientWrapper = delegateGrpcClientWrapper;
    }
    @Override
    public EcrResponseDTO getBuildDetails(IdentifierRef ecrConnectorRef, String imagePath, String registryHostname, String orgIdentifier, String projectIdentifier) {
        AwsConnectorDTO connector = getConnector(ecrConnectorRef);
        BaseNGAccess baseNGAccess =
                getBaseNGAccess(ecrConnectorRef.getAccountIdentifier(), orgIdentifier, projectIdentifier);
        List<EncryptedDataDetail> encryptionDetails = getEncryptionDetails(connector, baseNGAccess);
        EcrArtifactDelegateRequest ecrRequest = EcrArtifactDelegateRequest.builder()
                .awsConnectorDTO(connector)
                .encryptedDataDetails(encryptionDetails)
                .imagePath(imagePath)
                .sourceType(ArtifactSourceType.ECR)
                .region(AWS_DEFAULT_REGION)
                .build();
        ArtifactTaskExecutionResponse artifactTaskExecutionResponse = executeSyncTask(
                ecrRequest, ArtifactTaskType.GET_BUILDS, baseNGAccess, "Ecr Get Builds task failure due to error");
        return getEcrResponseDTO(artifactTaskExecutionResponse);
    }

    @Override
    public EcrBuildDetailsDTO getSuccessfulBuild(IdentifierRef dockerConnectorRef, String imagePath, EcrRequestDTO dockerRequestDTO, String orgIdentifier, String projectIdentifier) {
        return null;
    }

    @Override
    public boolean validateArtifactServer(IdentifierRef ecrConnectorRef, String imagePath, String orgIdentifier, String projectIdentifier, String registryHostname) {
        return false;
    }

    @Override
    public boolean validateArtifactSource(String imagePath, IdentifierRef ecrConnectorRef, String registryHostname, String orgIdentifier, String projectIdentifier) {
        return false;
    }

    private AwsConnectorDTO getConnector(IdentifierRef ecrConnectorRef) {
        Optional<ConnectorResponseDTO> connectorDTO = connectorService.get(ecrConnectorRef.getAccountIdentifier(),
                ecrConnectorRef.getOrgIdentifier(), ecrConnectorRef.getProjectIdentifier(), ecrConnectorRef.getIdentifier());

        if (!connectorDTO.isPresent() || !isAAwsConnector(connectorDTO.get())) {
            throw new InvalidRequestException(String.format("Connector not found for identifier : [%s] with scope: [%s]",
                    ecrConnectorRef.getIdentifier(), ecrConnectorRef.getScope()),
                    WingsException.USER);
        }
        ConnectorInfoDTO connectors = connectorDTO.get().getConnector();
        return (AwsConnectorDTO) connectors.getConnectorConfig();
    }
    private boolean isAAwsConnector(@Valid @NotNull ConnectorResponseDTO connectorResponseDTO) {
        return ConnectorType.AWS == (connectorResponseDTO.getConnector().getConnectorType());
    }
    private BaseNGAccess getBaseNGAccess(String accountId, String orgIdentifier, String projectIdentifier) {
        return BaseNGAccess.builder()
                .accountIdentifier(accountId)
                .orgIdentifier(orgIdentifier)
                .projectIdentifier(projectIdentifier)
                .build();
    }
    private List<EncryptedDataDetail> getEncryptionDetails(
            @Nonnull AwsConnectorDTO awsConnectorDTO, @Nonnull NGAccess ngAccess) {
        if (awsConnectorDTO.getCredential() != null && awsConnectorDTO.getCredential().getConfig() != null) {
            return secretManagerClientService.getEncryptionDetails(ngAccess, (AwsManualConfigSpecDTO)awsConnectorDTO.getCredential().getConfig());
        }
        return new ArrayList<>();
    }
    private ArtifactTaskExecutionResponse executeSyncTask(
            EcrArtifactDelegateRequest ecrRequest, ArtifactTaskType taskType, BaseNGAccess ngAccess, String ifFailedMessage) {
        DelegateResponseData responseData = getResponseData(ngAccess, ecrRequest, taskType);
        return getTaskExecutionResponse(responseData, ifFailedMessage);
    }
    private DelegateResponseData getResponseData(
            BaseNGAccess ngAccess, EcrArtifactDelegateRequest ecrRequest, ArtifactTaskType artifactTaskType) {
        ArtifactTaskParameters artifactTaskParameters = ArtifactTaskParameters.builder()
                .accountId(ngAccess.getAccountIdentifier())
                .artifactTaskType(artifactTaskType)
                .attributes(ecrRequest)
                .build();
        final DelegateTaskRequest delegateTaskRequest =
                DelegateTaskRequest.builder()
                        .accountId(ngAccess.getAccountIdentifier())
                        .taskType(NGTaskType.ECR_ARTIFACT_TASK_NG.name())
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
    private EcrResponseDTO getEcrResponseDTO(ArtifactTaskExecutionResponse artifactTaskExecutionResponse) {
        List<EcrArtifactDelegateResponse> ecrArtifactDelegateResponses =
                artifactTaskExecutionResponse.getArtifactDelegateResponses()
                        .stream()
                        .map(delegateResponse -> (EcrArtifactDelegateResponse) delegateResponse)
                        .collect(Collectors.toList());
        return EcrResourceMapper.toEcrResponse(ecrArtifactDelegateResponses);
    }
}
