package io.harness.delegate.task.artifacts.ecr;

import static io.harness.exception.WingsException.USER;

import static software.wings.helpers.ext.ecr.EcrService.MAX_NO_OF_TAGS_PER_IMAGE;

import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.comparator.BuildDetailsInternalComparatorDescending;
import io.harness.artifacts.ecr.beans.AwsInternalConfig;
import io.harness.artifacts.ecr.beans.EcrInternalConfig;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.task.artifacts.DelegateArtifactTaskHandler;
import io.harness.delegate.task.artifacts.mappers.EcrRequestResponseMapper;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.InvalidRequestException;

import software.wings.helpers.ext.ecr.EcrService;
import software.wings.service.impl.AwsApiHelperService;
import software.wings.service.impl.delegate.AwsEcrApiHelperServiceDelegateImpl;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
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
public class EcrArtifactTaskHandler extends DelegateArtifactTaskHandler<EcrArtifactDelegateRequest> {
  private final EcrService ecrService;
  private final AwsApiHelperService awsApiHelperService;
  @Inject AwsEcrApiHelperServiceDelegateImpl awsEcrApiHelperServiceDelegate;

  @Override
  public ArtifactTaskExecutionResponse getBuilds(EcrArtifactDelegateRequest attributesRequest) {
    List<BuildDetailsInternal> builds;
    EcrInternalConfig ecrInternalConfig;
    try {
      ecrInternalConfig = getEcrInternalConfig(attributesRequest);
    } catch (IOException e) {
      log.error("Could not get secret keys", e);
      throw new InvalidRequestException("Could not get secret keys - " + e.getMessage(), USER);
    }
    AwsInternalConfig awsInternalConfig =
        EcrRequestResponseMapper.toAwsInternalConfig(ecrInternalConfig.getCredentialsProvider());
    String ecrimageUrl = awsEcrApiHelperServiceDelegate.getEcrImageUrl(
        awsInternalConfig, attributesRequest.getRegion(), attributesRequest.getImagePath());
    builds = ecrService.getBuilds(awsInternalConfig, ecrimageUrl, attributesRequest.getRegion(),
        attributesRequest.getImagePath(), MAX_NO_OF_TAGS_PER_IMAGE);
    List<EcrArtifactDelegateResponse> ecrArtifactDelegateResponseList =
        builds.stream()
            .sorted(new BuildDetailsInternalComparatorDescending())
            .map(build -> EcrRequestResponseMapper.toEcrResponse(build, attributesRequest))
            .collect(Collectors.toList());
    return getSuccessTaskExecutionResponse(ecrArtifactDelegateResponseList);
  }

  @Override
  public ArtifactTaskExecutionResponse validateArtifactServer(EcrArtifactDelegateRequest attributesRequest) {
    EcrInternalConfig ecrInternalConfig;
    try {
      ecrInternalConfig = getEcrInternalConfig(attributesRequest);
    } catch (IOException e) {
      log.error("Could not get auth tokens", e);
      throw new InvalidRequestException("Could not get auth tokens - " + e.getMessage(), USER);
    }
    AwsInternalConfig awsInternalConfig =
        EcrRequestResponseMapper.toAwsInternalConfig(ecrInternalConfig.getCredentialsProvider());
    String ecrimageUrl = awsEcrApiHelperServiceDelegate.getEcrImageUrl(
        awsInternalConfig, attributesRequest.getRegion(), attributesRequest.getImagePath());
    boolean validateCredentials = ecrService.validateCredentials(
        awsInternalConfig, ecrimageUrl, attributesRequest.getRegion(), attributesRequest.getImagePath());
    return ArtifactTaskExecutionResponse.builder().isArtifactServerValid(validateCredentials).build();
  }

  @Override
  public ArtifactTaskExecutionResponse validateArtifactImage(EcrArtifactDelegateRequest attributesRequest) {
    EcrInternalConfig ecrInternalConfig;
    try {
      ecrInternalConfig = getEcrInternalConfig(attributesRequest);
    } catch (IOException e) {
      log.error("Could not get auth tokens", e);
      throw new InvalidRequestException("Could not get auth tokens - " + e.getMessage(), USER);
    }
    AwsInternalConfig awsInternalConfig =
        EcrRequestResponseMapper.toAwsInternalConfig(ecrInternalConfig.getCredentialsProvider());
    String ecrimageUrl = awsEcrApiHelperServiceDelegate.getEcrImageUrl(
        awsInternalConfig, attributesRequest.getRegion(), attributesRequest.getImagePath());
    boolean verifyImageName = ecrService.verifyImageName(
        awsInternalConfig, ecrimageUrl, attributesRequest.getRegion(), attributesRequest.getImagePath());
    return ArtifactTaskExecutionResponse.builder().isArtifactSourceValid(verifyImageName).build();
  }

  private ArtifactTaskExecutionResponse getSuccessTaskExecutionResponse(
      List<EcrArtifactDelegateResponse> responseList) {
    return ArtifactTaskExecutionResponse.builder()
        .artifactDelegateResponses(responseList)
        .isArtifactSourceValid(true)
        .isArtifactServerValid(true)
        .build();
  }

  @Override
  public ArtifactTaskExecutionResponse getLastSuccessfulBuild(EcrArtifactDelegateRequest attributesRequest) {
    BuildDetailsInternal lastSuccessfulBuild;
    EcrInternalConfig ecrInternalConfig;
    try {
      ecrInternalConfig = getEcrInternalConfig(attributesRequest);
    } catch (IOException e) {
      log.error("Could not get Auth keys", e);
      throw new InvalidRequestException("Could not get auth keys - " + e.getMessage(), USER);
    }
    AwsInternalConfig awsInternalConfig =
        EcrRequestResponseMapper.toAwsInternalConfig(ecrInternalConfig.getCredentialsProvider());
    String ecrimageUrl = awsEcrApiHelperServiceDelegate.getEcrImageUrl(
        awsInternalConfig, attributesRequest.getRegion(), attributesRequest.getImagePath());
    if (EmptyPredicate.isNotEmpty(attributesRequest.getTagRegex())) {
      lastSuccessfulBuild = ecrService.getLastSuccessfulBuildFromRegex(awsInternalConfig, ecrimageUrl,
          attributesRequest.getRegion(), attributesRequest.getImagePath(), attributesRequest.getTagRegex());
    } else {
      lastSuccessfulBuild = ecrService.verifyBuildNumber(awsInternalConfig, ecrimageUrl, attributesRequest.getRegion(),
          attributesRequest.getImagePath(), attributesRequest.getTag());
    }
    return getSuccessTaskExecutionResponse(
        Collections.singletonList(EcrRequestResponseMapper.toEcrResponse(lastSuccessfulBuild, attributesRequest)));
  }

  private EcrInternalConfig getEcrInternalConfig(EcrArtifactDelegateRequest attributesRequest) throws IOException {
    AwsInternalConfig awsInternalConfig = AwsInternalConfig.builder().build();
    if (attributesRequest.getAwsConnectorDTO() != null) {
      AwsCredentialDTO credential = attributesRequest.getAwsConnectorDTO().getCredential();
      AwsManualConfigSpecDTO awsManualConfigSpecDTO = (AwsManualConfigSpecDTO) credential.getConfig();
      awsInternalConfig =
          AwsInternalConfig.builder()
              .accessKey(awsManualConfigSpecDTO.getAccessKey().toCharArray())
              .secretKey(SecretRefHelper.getSecretConfigString(awsManualConfigSpecDTO.getSecretKeyRef()).toCharArray())
              .build();
    }
    AmazonCloudWatchClientBuilder builder =
        AmazonCloudWatchClientBuilder.standard().withRegion(attributesRequest.getRegion());
    awsApiHelperService.attachCredentialsAndBackoffPolicy(builder, awsInternalConfig);
    return EcrRequestResponseMapper.toEcrInternalConfig(attributesRequest, builder.getCredentials());
  }
}
