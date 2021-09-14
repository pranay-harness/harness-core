/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.artifacts.ecr;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.context.MdcGlobalContextData;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.eraro.ErrorCode;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionMetadataKeys;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.manage.GlobalContextManager;
import io.harness.security.encryption.SecretDecryptionService;

import com.amazonaws.AmazonServiceException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class EcrArtifactTaskHelper {
  private final EcrArtifactTaskHandler ecrArtifactTaskHandler;
  private final SecretDecryptionService secretDecryptionService;
  public ArtifactTaskResponse getArtifactCollectResponse(ArtifactTaskParameters artifactTaskParameters) {
    return getArtifactCollectResponse(artifactTaskParameters, null);
  }
  public ArtifactTaskResponse getArtifactCollectResponse(
      ArtifactTaskParameters artifactTaskParameters, LogCallback executionLogCallback) {
    EcrArtifactDelegateRequest attributes = (EcrArtifactDelegateRequest) artifactTaskParameters.getAttributes();
    decryptRequestDTOs(attributes);
    ArtifactTaskResponse artifactTaskResponse;
    try {
      switch (artifactTaskParameters.getArtifactTaskType()) {
        case GET_LAST_SUCCESSFUL_BUILD:
          saveLogs(executionLogCallback, "Fetching Artifact details");
          artifactTaskResponse = getSuccessTaskResponse(ecrArtifactTaskHandler.getLastSuccessfulBuild(attributes));
          EcrArtifactDelegateResponse ecrArtifactDelegateResponse =
              (EcrArtifactDelegateResponse) artifactTaskResponse.getArtifactTaskExecutionResponse()
                  .getArtifactDelegateResponses()
                  .get(0);
          saveLogs(executionLogCallback,
              "Fetched Artifact details \n  type: Ecr\n  imagePath: " + ecrArtifactDelegateResponse.getImagePath()
                  + "\n  tag: " + ecrArtifactDelegateResponse.getTag());
          break;
        case GET_BUILDS:
          saveLogs(executionLogCallback, "Fetching artifact details");
          artifactTaskResponse = getSuccessTaskResponse(ecrArtifactTaskHandler.getBuilds(attributes));
          saveLogs(executionLogCallback,
              "Fetched " + artifactTaskResponse.getArtifactTaskExecutionResponse().getArtifactDelegateResponses().size()
                  + " artifacts");
          break;
        case VALIDATE_ARTIFACT_SERVER:
          saveLogs(executionLogCallback, "Validating Artifact Server");
          artifactTaskResponse = getSuccessTaskResponse(ecrArtifactTaskHandler.validateArtifactServer(attributes));
          saveLogs(executionLogCallback,
              "validated artifact xxxxxxxx "
                  + ((EcrArtifactDelegateResponse) artifactTaskResponse.getArtifactTaskExecutionResponse()
                          .getArtifactDelegateResponses()
                          .get(0))
                        .getImageUrl());
          break;
        case VALIDATE_ARTIFACT_SOURCE:
          saveLogs(executionLogCallback, "Validating Artifact Source");
          artifactTaskResponse = getSuccessTaskResponse(ecrArtifactTaskHandler.validateArtifactImage(attributes));
          saveLogs(executionLogCallback,
              "validated artifact source: "
                  + ((EcrArtifactDelegateResponse) artifactTaskResponse.getArtifactTaskExecutionResponse()
                          .getArtifactDelegateResponses()
                          .get(0))
                        .getImageUrl());
          break;
        case GET_IMAGE_URL:
          saveLogs(executionLogCallback, "Fetching image URL");
          artifactTaskResponse = getSuccessTaskResponse(ecrArtifactTaskHandler.getEcrImageUrl(attributes));
          saveLogs(executionLogCallback,
              "Fetching image URL:"
                  + ((EcrArtifactDelegateResponse) artifactTaskResponse.getArtifactTaskExecutionResponse()
                          .getArtifactDelegateResponses()
                          .get(0))
                        .getImageUrl());
          break;
        case GET_AUTH_TOKEN:
          saveLogs(executionLogCallback, "Fetching Authentication token");
          artifactTaskResponse = getSuccessTaskResponse(ecrArtifactTaskHandler.getAmazonEcrAuthToken(attributes));
          saveLogs(executionLogCallback, "fetched Authentication token ***");
          break;
        case GET_IMAGES:
          saveLogs(executionLogCallback, "Fetching artifact images");
          artifactTaskResponse = getSuccessTaskResponse(ecrArtifactTaskHandler.getImages(attributes));
          saveLogs(executionLogCallback,
              "Fetched " + artifactTaskResponse.getArtifactTaskExecutionResponse().getArtifactImages().size()
                  + " images");
          break;
        default:
          saveLogs(executionLogCallback,
              "No corresponding Ecr artifact task type [{}]: " + artifactTaskParameters.toString());
          log.error("No corresponding Ecr artifact task type [{}]", artifactTaskParameters.toString());
          return ArtifactTaskResponse.builder()
              .commandExecutionStatus(CommandExecutionStatus.FAILURE)
              .errorMessage("There is no Ecr artifact task type impl defined for - "
                  + artifactTaskParameters.getArtifactTaskType().name())
              .errorCode(ErrorCode.INVALID_ARGUMENT)
              .build();
      }
    } catch (AmazonServiceException ex) {
      if (GlobalContextManager.get(MdcGlobalContextData.MDC_ID) != null) {
        ((MdcGlobalContextData) GlobalContextManager.get(MdcGlobalContextData.MDC_ID))
            .getMap()
            .put(ExceptionMetadataKeys.CONNECTOR.name(), attributes.getConnectorRef());
      }
      throw ex;
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
  private void saveLogs(LogCallback executionLogCallback, String message) {
    if (executionLogCallback != null) {
      executionLogCallback.saveExecutionLog(message);
    }
  }
}
