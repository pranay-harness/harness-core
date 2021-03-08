package io.harness.delegate.task.artifacts.gcr;

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

public class GcrArtifactTaskHelper {
  private final GcrArtifactTaskHandler gcrArtifactTaskHandler;
  private final SecretDecryptionService secretDecryptionService;

  public ArtifactTaskResponse getArtifactCollectResponse(ArtifactTaskParameters artifactTaskParameters) {
    GcrArtifactDelegateRequest attributes = (GcrArtifactDelegateRequest) artifactTaskParameters.getAttributes();
    decryptRequestDTOs(attributes);
    ArtifactTaskResponse artifactTaskResponse;
    switch (artifactTaskParameters.getArtifactTaskType()) {
      case GET_LAST_SUCCESSFUL_BUILD:
        artifactTaskResponse = getSuccessTaskResponse(gcrArtifactTaskHandler.getLastSuccessfulBuild(attributes));
        break;
      case GET_BUILDS:
        artifactTaskResponse = getSuccessTaskResponse(gcrArtifactTaskHandler.getBuilds(attributes));
        break;
      case VALIDATE_ARTIFACT_SERVER:
        artifactTaskResponse = getSuccessTaskResponse(gcrArtifactTaskHandler.validateArtifactServer(attributes));
        break;
      case VALIDATE_ARTIFACT_SOURCE:
        artifactTaskResponse = getSuccessTaskResponse(gcrArtifactTaskHandler.validateArtifactImage(attributes));
        break;
      default:
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
}
