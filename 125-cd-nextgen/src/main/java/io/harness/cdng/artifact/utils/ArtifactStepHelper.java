package io.harness.cdng.artifact.utils;

import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.EcrArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.GcrArtifactConfig;
import io.harness.cdng.artifact.mappers.ArtifactConfigToDelegateReqMapper;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.task.artifacts.ArtifactSourceDelegateRequest;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.ng.core.NGAccess;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.utils.IdentifierRefHelper;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class ArtifactStepHelper {
  @Named(DEFAULT_CONNECTOR_SERVICE) @Inject private ConnectorService connectorService;
  @Inject private SecretManagerClientService secretManagerClientService;

  public ArtifactSourceDelegateRequest toSourceDelegateRequest(ArtifactConfig artifactConfig, Ambiance ambiance) {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    ConnectorInfoDTO connectorDTO;
    NGAccess ngAccess = AmbianceHelper.getNgAccess(ambiance);
    switch (artifactConfig.getSourceType()) {
      case DOCKER_REGISTRY:
        DockerHubArtifactConfig dockerConfig = (DockerHubArtifactConfig) artifactConfig;
        connectorDTO = getConnector(dockerConfig.getConnectorRef().getValue(), ambiance);
        DockerConnectorDTO connectorConfig = (DockerConnectorDTO) connectorDTO.getConnectorConfig();
        if (connectorConfig.getAuth() != null && connectorConfig.getAuth().getCredentials() != null) {
          encryptedDataDetails =
              secretManagerClientService.getEncryptionDetails(ngAccess, connectorConfig.getAuth().getCredentials());
        }
        return ArtifactConfigToDelegateReqMapper.getDockerDelegateRequest(
            dockerConfig, connectorConfig, encryptedDataDetails);
      case GCR:
        GcrArtifactConfig gcrArtifactConfig = (GcrArtifactConfig) artifactConfig;
        connectorDTO = getConnector(gcrArtifactConfig.getConnectorRef().getValue(), ambiance);
        GcpConnectorDTO gcpConnectorDTO = (GcpConnectorDTO) connectorDTO.getConnectorConfig();
        if (gcpConnectorDTO.getCredential() != null && gcpConnectorDTO.getCredential().getConfig() != null) {
          encryptedDataDetails =
              secretManagerClientService.getEncryptionDetails(ngAccess, gcpConnectorDTO.getCredential().getConfig());
        }
        return ArtifactConfigToDelegateReqMapper.getGcrDelegateRequest(
            gcrArtifactConfig, gcpConnectorDTO, encryptedDataDetails);
      case ECR:
        EcrArtifactConfig ecrArtifactConfig = (EcrArtifactConfig) artifactConfig;
        connectorDTO = getConnector(ecrArtifactConfig.getConnectorRef().getValue(), ambiance);
        AwsConnectorDTO awsConnectorDTO = (AwsConnectorDTO) connectorDTO.getConnectorConfig();
        if (awsConnectorDTO.getCredential() != null
            && awsConnectorDTO.getCredential().getConfig() instanceof DecryptableEntity) {
          encryptedDataDetails = secretManagerClientService.getEncryptionDetails(
              ngAccess, (DecryptableEntity) awsConnectorDTO.getCredential().getConfig());
        }
        return ArtifactConfigToDelegateReqMapper.getEcrDelegateRequest(
            ecrArtifactConfig, awsConnectorDTO, encryptedDataDetails);
      default:
        throw new UnsupportedOperationException(
            String.format("Unknown Artifact Config type: [%s]", artifactConfig.getSourceType()));
    }
  }

  private ConnectorInfoDTO getConnector(String connectorIdentifierRef, Ambiance ambiance) {
    NGAccess ngAccess = AmbianceHelper.getNgAccess(ambiance);
    IdentifierRef connectorRef = IdentifierRefHelper.getIdentifierRef(connectorIdentifierRef,
        ngAccess.getAccountIdentifier(), ngAccess.getOrgIdentifier(), ngAccess.getProjectIdentifier());
    Optional<ConnectorResponseDTO> connectorDTO = connectorService.get(connectorRef.getAccountIdentifier(),
        connectorRef.getOrgIdentifier(), connectorRef.getProjectIdentifier(), connectorRef.getIdentifier());
    if (!connectorDTO.isPresent()) {
      throw new InvalidRequestException(
          String.format("Connector not found for identifier : [%s]", connectorIdentifierRef), WingsException.USER);
    }
    return connectorDTO.get().getConnector();
  }

  public TaskType getArtifactStepTaskType(ArtifactConfig artifactConfig) {
    switch (artifactConfig.getSourceType()) {
      case DOCKER_REGISTRY:
        return TaskType.DOCKER_ARTIFACT_TASK_NG;
      case GCR:
        return TaskType.GCR_ARTIFACT_TASK_NG;
      case ECR:
        return TaskType.ECR_ARTIFACT_TASK_NG;
      default:
        throw new UnsupportedOperationException(
            String.format("Unknown Artifact Config type: [%s]", artifactConfig.getSourceType()));
    }
  }
}
