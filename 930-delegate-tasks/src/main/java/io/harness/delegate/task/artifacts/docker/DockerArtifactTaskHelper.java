package io.harness.delegate.task.artifacts.docker;

import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
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

  public ArtifactTaskResponse getArtifactCollectResponse(
      ArtifactTaskParameters artifactTaskParameters, LogCallback executionLogCallback) {
    try {
      DockerArtifactDelegateRequest attributes = (DockerArtifactDelegateRequest) artifactTaskParameters.getAttributes();
      String registryUrl = attributes.getDockerConnectorDTO().getDockerRegistryUrl();
      decryptRequestDTOs(attributes);
      ArtifactTaskResponse artifactTaskResponse;
      switch (artifactTaskParameters.getArtifactTaskType()) {
        case GET_LAST_SUCCESSFUL_BUILD:
          saveLogs(executionLogCallback, "Fetching Artifact details");
          artifactTaskResponse = getSuccessTaskResponse(dockerArtifactTaskHandler.getLastSuccessfulBuild(attributes));
          DockerArtifactDelegateResponse dockerArtifactDelegateResponse =
              (DockerArtifactDelegateResponse) (artifactTaskResponse.getArtifactTaskExecutionResponse()
                                                    .getArtifactDelegateResponses()
                                                    .size()
                          != 0
                      ? artifactTaskResponse.getArtifactTaskExecutionResponse().getArtifactDelegateResponses().get(0)
                      : DockerArtifactDelegateResponse.builder().build());
          saveLogs(executionLogCallback,
              "Fetched Artifact details \n  type: Dockerhub\n  imagePath: "
                  + dockerArtifactDelegateResponse.getImagePath()
                  + "\n  tag: " + dockerArtifactDelegateResponse.getTag());
          break;
        case GET_BUILDS:
          saveLogs(executionLogCallback, "Fetching artifact details");
          artifactTaskResponse = getSuccessTaskResponse(dockerArtifactTaskHandler.getBuilds(attributes));
          saveLogs(executionLogCallback,
              "Fetched " + artifactTaskResponse.getArtifactTaskExecutionResponse().getArtifactDelegateResponses().size()
                  + " artifacts");
          break;
        case GET_LABELS:
          saveLogs(executionLogCallback, "Fetching labels");
          artifactTaskResponse = getSuccessTaskResponse(dockerArtifactTaskHandler.getLabels(attributes));
          saveLogs(executionLogCallback,
              "Fetched labels: "
                  + artifactTaskResponse.getArtifactTaskExecutionResponse().getArtifactDelegateResponses().toString());
          break;
        case VALIDATE_ARTIFACT_SERVER:
          saveLogs(executionLogCallback, "Validating  Artifact Server");
          artifactTaskResponse = getSuccessTaskResponse(dockerArtifactTaskHandler.validateArtifactServer(attributes));
          saveLogs(executionLogCallback, "validated artifact xxxxxxxx " + registryUrl);
          break;
        case VALIDATE_ARTIFACT_SOURCE:
          saveLogs(executionLogCallback, "Validating Artifact Source");
          artifactTaskResponse = getSuccessTaskResponse(dockerArtifactTaskHandler.validateArtifactImage(attributes));
          saveLogs(executionLogCallback,
              "Artifact Source is valid: " + registryUrl + (registryUrl.endsWith("/") ? "" : "/")
                  + attributes.getImagePath());
          break;
        default:
          saveLogs(executionLogCallback,
              "No corresponding Docker artifact task type [{}]: " + artifactTaskParameters.toString());
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
  private void saveLogs(LogCallback executionLogCallback, String message) {
    if (executionLogCallback != null) {
      executionLogCallback.saveExecutionLog(message);
    }
  }
  public ArtifactTaskResponse getArtifactCollectResponse(ArtifactTaskParameters artifactTaskParameters) {
    return getArtifactCollectResponse(artifactTaskParameters, null);
  }
}
