package io.harness.delegate.task.artifacts.docker;

import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
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
public class DockerArtifactTaskHelper {
  private final DockerArtifactTaskHandler dockerArtifactTaskHandler;
  private final SecretDecryptionService secretDecryptionService;

  public ArtifactTaskResponse getArtifactCollectResponse(ArtifactTaskParameters artifactTaskParameters) {
    try {
      DockerArtifactDelegateRequest attributes = (DockerArtifactDelegateRequest) artifactTaskParameters.getAttributes();
      decryptRequestDTOs(attributes);
      ArtifactTaskResponse artifactTaskResponse;
      switch (artifactTaskParameters.getArtifactTaskType()) {
        case GET_LAST_SUCCESSFUL_BUILD:
          artifactTaskResponse = getSuccessTaskResponse(dockerArtifactTaskHandler.getLastSuccessfulBuild(attributes));
          break;
        case GET_BUILDS:
          artifactTaskResponse = getSuccessTaskResponse(dockerArtifactTaskHandler.getBuilds(attributes));
          break;
        case GET_LABELS:
          artifactTaskResponse = getSuccessTaskResponse(dockerArtifactTaskHandler.getLabels(attributes));
          break;
        case VALIDATE_ARTIFACT_SERVER:
          artifactTaskResponse = getSuccessTaskResponse(dockerArtifactTaskHandler.validateArtifactServer(attributes));
          break;
        case VALIDATE_ARTIFACT_SOURCE:
          artifactTaskResponse = getSuccessTaskResponse(dockerArtifactTaskHandler.validateArtifactImage(attributes));
          break;
        default:
          log.error("No corresponding Docker artifact task type [{}]", artifactTaskParameters.toString());
          return ArtifactTaskResponse.builder()
              .commandExecutionStatus(CommandExecutionStatus.FAILURE)
              .errorMessage("There is no Docker artifact task type impl defined for - "
                  + artifactTaskParameters.getArtifactTaskType().name())
              .errorCode(ErrorCode.INVALID_ARGUMENT)
              .build();
      }
      return artifactTaskResponse;
    } catch (Exception ex) {
      log.error("Exception in processing Docker artifact task [{}]", artifactTaskParameters.toString(), ex);
      return ArtifactTaskResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ExceptionUtils.getMessage(ex))
          .errorCode(ErrorCode.INVALID_ARGUMENT)
          .build();
    }
  }

  private ArtifactTaskResponse getSuccessTaskResponse(ArtifactTaskExecutionResponse taskExecutionResponse) {
    return ArtifactTaskResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .artifactTaskExecutionResponse(taskExecutionResponse)
        .build();
  }

  private void decryptRequestDTOs(DockerArtifactDelegateRequest dockerRequest) {
    if (dockerRequest.getDockerConnectorDTO().getAuth() != null) {
      secretDecryptionService.decrypt(
          dockerRequest.getDockerConnectorDTO().getAuth().getCredentials(), dockerRequest.getEncryptedDataDetails());
    }
  }
}
