package io.harness.delegate.task.artifacts.gcr;

import static io.harness.data.structure.HasPredicate.hasSome;
import static io.harness.exception.WingsException.USER;

import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.comparator.BuildDetailsInternalComparatorDescending;
import io.harness.artifacts.gcr.beans.GcrInternalConfig;
import io.harness.artifacts.gcr.service.GcrApiService;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorCredentialDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.task.artifacts.DelegateArtifactTaskHandler;
import io.harness.delegate.task.artifacts.mappers.GcrRequestResponseMapper;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.delegate.task.gcp.helpers.GcpHelperService;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class GcrArtifactTaskHandler extends DelegateArtifactTaskHandler<GcrArtifactDelegateRequest> {
  private final GcrApiService gcrService;
  private final GcpHelperService gcpHelperService;

  @Override
  public ArtifactTaskExecutionResponse getBuilds(GcrArtifactDelegateRequest attributesRequest) {
    List<BuildDetailsInternal> builds;
    GcrInternalConfig gcrInternalConfig;
    try {
      gcrInternalConfig = getGcrInternalConfig(attributesRequest);
    } catch (IOException e) {
      log.error("Could not get basic auth header", e);
      throw new InvalidRequestException("Could not get basic auth header - " + e.getMessage(), USER);
    }
    builds = gcrService.getBuilds(
        gcrInternalConfig, attributesRequest.getImagePath(), GcrApiService.MAX_NO_OF_TAGS_PER_IMAGE);
    List<GcrArtifactDelegateResponse> gcrArtifactDelegateResponseList =
        builds.stream()
            .sorted(new BuildDetailsInternalComparatorDescending())
            .map(build -> GcrRequestResponseMapper.toGcrResponse(build, attributesRequest))
            .collect(Collectors.toList());
    return getSuccessTaskExecutionResponse(gcrArtifactDelegateResponseList);
  }

  @Override
  public ArtifactTaskExecutionResponse getLastSuccessfulBuild(GcrArtifactDelegateRequest attributesRequest) {
    BuildDetailsInternal lastSuccessfulBuild;
    GcrInternalConfig gcrInternalConfig;
    try {
      gcrInternalConfig = getGcrInternalConfig(attributesRequest);
    } catch (IOException e) {
      log.error("Could not get basic auth header", e);
      throw new InvalidRequestException("Could not get basic auth header - " + e.getMessage(), USER);
    }
    if (hasSome(attributesRequest.getTagRegex())) {
      lastSuccessfulBuild = gcrService.getLastSuccessfulBuildFromRegex(
          gcrInternalConfig, attributesRequest.getImagePath(), attributesRequest.getTagRegex());
    } else {
      lastSuccessfulBuild =
          gcrService.verifyBuildNumber(gcrInternalConfig, attributesRequest.getImagePath(), attributesRequest.getTag());
    }
    return getSuccessTaskExecutionResponse(
        Collections.singletonList(GcrRequestResponseMapper.toGcrResponse(lastSuccessfulBuild, attributesRequest)));
  }

  @Override
  public ArtifactTaskExecutionResponse validateArtifactServer(GcrArtifactDelegateRequest attributesRequest) {
    GcrInternalConfig gcrInternalConfig;
    try {
      gcrInternalConfig = getGcrInternalConfig(attributesRequest);
    } catch (IOException e) {
      log.error("Could not get basic auth header", e);
      throw new InvalidRequestException("Could not get basic auth header - " + e.getMessage(), USER);
    }
    boolean validateCredentials = gcrService.validateCredentials(gcrInternalConfig, attributesRequest.getImagePath());
    return ArtifactTaskExecutionResponse.builder().isArtifactServerValid(validateCredentials).build();
  }

  @Override
  public ArtifactTaskExecutionResponse validateArtifactImage(GcrArtifactDelegateRequest attributesRequest) {
    GcrInternalConfig gcrInternalConfig;
    try {
      gcrInternalConfig = getGcrInternalConfig(attributesRequest);
    } catch (IOException e) {
      log.error("Could not get basic auth header", e);
      throw new InvalidRequestException("Could not get basic auth header - " + e.getMessage(), USER);
    }
    boolean verifyImageName = gcrService.verifyImageName(gcrInternalConfig, attributesRequest.getImagePath());
    return ArtifactTaskExecutionResponse.builder().isArtifactSourceValid(verifyImageName).build();
  }

  private ArtifactTaskExecutionResponse getSuccessTaskExecutionResponse(
      List<GcrArtifactDelegateResponse> responseList) {
    return ArtifactTaskExecutionResponse.builder()
        .artifactDelegateResponses(responseList)
        .isArtifactSourceValid(true)
        .isArtifactServerValid(true)
        .build();
  }

  private GcrInternalConfig getGcrInternalConfig(GcrArtifactDelegateRequest attributesRequest) throws IOException {
    char[] serviceAccountKeyFileContent = new char[0];
    boolean isUseDelegate = false;

    if (attributesRequest.getGcpConnectorDTO() != null) {
      GcpConnectorCredentialDTO credential = attributesRequest.getGcpConnectorDTO().getCredential();
      if (credential.getGcpCredentialType() == GcpCredentialType.INHERIT_FROM_DELEGATE) {
        isUseDelegate = true;
      } else {
        serviceAccountKeyFileContent =
            ((GcpManualDetailsDTO) credential.getConfig()).getSecretKeyRef().getDecryptedValue();
      }
    }
    return GcrRequestResponseMapper.toGcrInternalConfig(
        attributesRequest, gcpHelperService.getBasicAuthHeader(serviceAccountKeyFileContent, isUseDelegate));
  }
}
