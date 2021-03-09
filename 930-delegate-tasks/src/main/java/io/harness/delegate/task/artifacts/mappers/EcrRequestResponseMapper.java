package io.harness.delegate.task.artifacts.mappers;

import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.ecr.beans.AwsInternalConfig;
import io.harness.artifacts.ecr.beans.EcrInternalConfig;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.ecr.EcrArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.ecr.EcrArtifactDelegateResponse;

import com.amazonaws.auth.AWSCredentialsProvider;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.experimental.UtilityClass;

@UtilityClass
public class EcrRequestResponseMapper {
  public EcrInternalConfig toEcrInternalConfig(
      EcrArtifactDelegateRequest request, AWSCredentialsProvider credentialsProvider) {
    return EcrInternalConfig.builder().region(request.getRegion()).credentialsProvider(credentialsProvider).build();
  }

  public AwsInternalConfig toAwsInternalConfig(AWSCredentialsProvider credentialsProvider) {
    return AwsInternalConfig.builder()
        .accessKey(credentialsProvider.getCredentials().getAWSAccessKeyId().toCharArray())
        .secretKey(credentialsProvider.getCredentials().getAWSSecretKey().toCharArray())
        .build();
  }

  public EcrArtifactDelegateResponse toEcrResponse(
      BuildDetailsInternal buildDetailsInternal, EcrArtifactDelegateRequest request) {
    return EcrArtifactDelegateResponse.builder()
        .buildDetails(ArtifactBuildDetailsMapper.toBuildDetailsNG(buildDetailsInternal))
        .imagePath(request.getImagePath())
        .tag(buildDetailsInternal.getNumber())
        .sourceType(ArtifactSourceType.ECR)
        .build();
  }
  public List<EcrArtifactDelegateResponse> toEcrResponse(
      List<Map<String, String>> labelsList, EcrArtifactDelegateRequest request) {
    return IntStream.range(0, request.getTagsList().size())
        .mapToObj(i
            -> EcrArtifactDelegateResponse.builder()
                   .buildDetails(
                       ArtifactBuildDetailsMapper.toBuildDetailsNG(labelsList.get(i), request.getTagsList().get(i)))
                   .imagePath(request.getImagePath())
                   .sourceType(ArtifactSourceType.ECR)
                   .build())
        .collect(Collectors.toList());
  }
}
