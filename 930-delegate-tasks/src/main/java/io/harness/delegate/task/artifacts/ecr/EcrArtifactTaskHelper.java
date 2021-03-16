package io.harness.delegate.task.artifacts.ecr;

import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.eraro.ErrorCode;
import io.harness.logging.CommandExecutionStatus;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
public class EcrArtifactTaskHelper {
  private final EcrArtifactTaskHandler ecrArtifactTaskHandler;
  private final SecretDecryptionService secretDecryptionService;
  public ArtifactTaskResponse getArtifactCollectResponse(ArtifactTaskParameters artifactTaskParameters) {
    EcrArtifactDelegateRequest attributes = (EcrArtifactDelegateRequest) artifactTaskParameters.getAttributes();
    decryptRequestDTOs(attributes);
    ArtifactTaskResponse artifactTaskResponse;
    switch (artifactTaskParameters.getArtifactTaskType()) {
      case GET_LAST_SUCCESSFUL_BUILD:
        artifactTaskResponse = getSuccessTaskResponse(ecrArtifactTaskHandler.getLastSuccessfulBuild(attributes));
        break;
      case GET_BUILDS:
        artifactTaskResponse = getSuccessTaskResponse(ecrArtifactTaskHandler.getBuilds(attributes));
        break;
      case VALIDATE_ARTIFACT_SERVER:
        artifactTaskResponse = getSuccessTaskResponse(ecrArtifactTaskHandler.validateArtifactServer(attributes));
        break;
      case VALIDATE_ARTIFACT_SOURCE:
        artifactTaskResponse = getSuccessTaskResponse(ecrArtifactTaskHandler.validateArtifactImage(attributes));
        break;
      case GET_IMAGE_URL:
        artifactTaskResponse = getSuccessTaskResponse(ecrArtifactTaskHandler.getEcrImageUrl(attributes));
        break;
      case GET_AUTH_TOKEN:
        artifactTaskResponse = getSuccessTaskResponse(ecrArtifactTaskHandler.getAmazonEcrAuthToken(attributes));
        break;
      default:
        log.error("No corresponding Ecr artifact task type [{}]", artifactTaskParameters.toString());
        return ArtifactTaskResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.FAILURE)
            .errorMessage("There is no Ecr artifact task type impl defined for - "
                + artifactTaskParameters.getArtifactTaskType().name())
            .errorCode(ErrorCode.INVALID_ARGUMENT)
            .build();
    }
    return artifactTaskResponse;
  }
  private void decryptRequestDTOs(EcrArtifactDelegateRequest ecrRequest) {
    if (ecrRequest.getAwsConnectorDTO().getCredential() != null
        && ecrRequest.getAwsConnectorDTO().getCredential().getConfig() != null) {
      secretDecryptionService.decrypt(
          (AwsManualConfigSpecDTO) ecrRequest.getAwsConnectorDTO().getCredential().getConfig(),
          ecrRequest.getEncryptedDataDetails());
    }
  }
  private ArtifactTaskResponse getSuccessTaskResponse(ArtifactTaskExecutionResponse taskExecutionResponse) {
    return ArtifactTaskResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .artifactTaskExecutionResponse(taskExecutionResponse)
        .build();
  }
}
