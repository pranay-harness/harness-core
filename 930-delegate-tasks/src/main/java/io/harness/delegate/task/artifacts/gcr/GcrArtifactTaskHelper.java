package io.harness.delegate.task.artifacts.gcr;

import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.eraro.ErrorCode;
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

public class GcrArtifactTaskHelper {
  private final GcrArtifactTaskHandler gcrArtifactTaskHandler;
  private final SecretDecryptionService secretDecryptionService;

  public ArtifactTaskResponse getArtifactCollectResponse(ArtifactTaskParameters artifactTaskParameters) {
    return getArtifactCollectResponse(artifactTaskParameters, null);
  }
  public ArtifactTaskResponse getArtifactCollectResponse(
      ArtifactTaskParameters artifactTaskParameters, LogCallback executionLogCallback) {
    GcrArtifactDelegateRequest attributes = (GcrArtifactDelegateRequest) artifactTaskParameters.getAttributes();
    String registryUrl = attributes.getRegistryHostname();
    decryptRequestDTOs(attributes);
    ArtifactTaskResponse artifactTaskResponse;
    switch (artifactTaskParameters.getArtifactTaskType()) {
      case GET_LAST_SUCCESSFUL_BUILD:
        saveLogs(executionLogCallback, "Fetching Artifact details");
        artifactTaskResponse = getSuccessTaskResponse(gcrArtifactTaskHandler.getLastSuccessfulBuild(attributes));
        GcrArtifactDelegateResponse gcrArtifactDelegateResponse =
            (GcrArtifactDelegateResponse) artifactTaskResponse.getArtifactTaskExecutionResponse()
                .getArtifactDelegateResponses()
                .get(0);
        saveLogs(executionLogCallback,
            "Fetched Artifact details \n  type: Gcr\n  imagePath: " + gcrArtifactDelegateResponse.getImagePath()
                + "\n  tag: " + gcrArtifactDelegateResponse.getTag());
        break;
      case GET_BUILDS:
        saveLogs(executionLogCallback, "Fetching artifact details");
        artifactTaskResponse = getSuccessTaskResponse(gcrArtifactTaskHandler.getBuilds(attributes));
        saveLogs(executionLogCallback,
            "Fetched " + artifactTaskResponse.getArtifactTaskExecutionResponse().getArtifactDelegateResponses().size()
                + " artifacts");
        break;
      case VALIDATE_ARTIFACT_SERVER:
        saveLogs(executionLogCallback, "Validating Artifact Server");
        artifactTaskResponse = getSuccessTaskResponse(gcrArtifactTaskHandler.validateArtifactServer(attributes));
        saveLogs(executionLogCallback, "validated artifact xxxxxxxx " + registryUrl);
        break;
      case VALIDATE_ARTIFACT_SOURCE:
        saveLogs(executionLogCallback, "Validating Artifact Source");
        artifactTaskResponse = getSuccessTaskResponse(gcrArtifactTaskHandler.validateArtifactImage(attributes));
        saveLogs(executionLogCallback,
            "Artifact Source is valid: " + registryUrl + (registryUrl.endsWith("/") ? "" : "/")
                + attributes.getImagePath());
        break;
      default:
        saveLogs(
            executionLogCallback, "No corresponding Gcr artifact task type [{}]: " + artifactTaskParameters.toString());
        log.error("No corresponding Gcr artifact task type [{}]", artifactTaskParameters.toString());
        return ArtifactTaskResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.FAILURE)
            .errorMessage("There is no Gcr artifact task type impl defined for - "
                + artifactTaskParameters.getArtifactTaskType().name())
            .errorCode(ErrorCode.INVALID_ARGUMENT)
            .build();
    }
    return artifactTaskResponse;
  }
  private void decryptRequestDTOs(GcrArtifactDelegateRequest gcrRequest) {
    if (gcrRequest.getGcpConnectorDTO().getCredential() != null
        && gcrRequest.getGcpConnectorDTO().getCredential().getConfig() != null) {
      secretDecryptionService.decrypt(
          gcrRequest.getGcpConnectorDTO().getCredential().getConfig(), gcrRequest.getEncryptedDataDetails());
    }
  }
  private ArtifactTaskResponse getSuccessTaskResponse(ArtifactTaskExecutionResponse taskExecutionResponse) {
    return ArtifactTaskResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .artifactTaskExecutionResponse(taskExecutionResponse)
        .build();
  }
  private void saveLogs(LogCallback executionLogCallback, String message) {
    if (executionLogCallback != null) {
      executionLogCallback.saveExecutionLog(message);
    }
  }
}
