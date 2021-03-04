package software.wings.helpers.ext.ecr;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;

import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.GeneralException;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.common.BuildDetailsComparatorAscending;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.helpers.ext.jenkins.BuildDetails.BuildDetailsMetadataKeys;
import software.wings.service.impl.AwsApiHelperService;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.aws.delegate.AwsEcrHelperServiceDelegate;
import software.wings.service.intfc.security.EncryptionService;

import com.amazonaws.services.ecr.model.DescribeRepositoriesRequest;
import com.amazonaws.services.ecr.model.DescribeRepositoriesResult;
import com.amazonaws.services.ecr.model.ListImagesRequest;
import com.amazonaws.services.ecr.model.ListImagesResult;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by brett on 7/15/17
 */
@OwnedBy(CDC)
@Singleton
public class EcrServiceImpl implements EcrService {
  @Inject private AwsHelperService awsHelperService;
  @Inject private EncryptionService encryptionService;
  @Inject private AwsEcrHelperServiceDelegate ecrServiceDelegate;
  @Inject private AwsApiHelperService awsApiHelperService;

  @Override
  public List<BuildDetails> getBuilds(
      AwsInternalConfig awsConfig, String imageUrl, String region, String imageName, int maxNumberOfBuilds) {
    List<BuildDetails> buildDetails = new ArrayList<>();
    try {
      ListImagesResult listImagesResult;
      ListImagesRequest listImagesRequest = new ListImagesRequest().withRepositoryName(imageName);
      do {
        listImagesResult = awsApiHelperService.listEcrImages(awsConfig, region, listImagesRequest);
        listImagesResult.getImageIds()
            .stream()
            .filter(imageIdentifier -> imageIdentifier != null && isNotEmpty(imageIdentifier.getImageTag()))
            .forEach(imageIdentifier -> {
              Map<String, String> metadata = new HashMap();
              metadata.put(BuildDetailsMetadataKeys.image, imageUrl + ":" + imageIdentifier.getImageTag());
              metadata.put(BuildDetailsMetadataKeys.tag, imageIdentifier.getImageTag());
              buildDetails.add(aBuildDetails()
                                   .withNumber(imageIdentifier.getImageTag())
                                   .withMetadata(metadata)
                                   .withUiDisplayName("Tag# " + imageIdentifier.getImageTag())
                                   .build());
            });
        listImagesRequest.setNextToken(listImagesResult.getNextToken());
      } while (listImagesRequest.getNextToken() != null);
    } catch (Exception e) {
      throw new GeneralException(ExceptionUtils.getMessage(e), USER);
    }
    // Sorting at build tag for docker artifacts.
    return buildDetails.stream().sorted(new BuildDetailsComparatorAscending()).collect(toList());
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(AwsConfig awsConfig, String imageName) {
    return null;
  }

  @Override
  public boolean verifyRepository(AwsInternalConfig awsConfig, String region, String repositoryName) {
    return listEcrRegistry(awsConfig, region).contains(repositoryName);
  }

  @Override
  public List<String> listRegions(AwsInternalConfig awsConfig) {
    return awsApiHelperService.listRegions(awsConfig);
  }

  @Override
  public List<String> listEcrRegistry(AwsInternalConfig awsConfig, String region) {
    List<String> repoNames = new ArrayList<>();
    DescribeRepositoriesRequest describeRepositoriesRequest = new DescribeRepositoriesRequest();
    DescribeRepositoriesResult describeRepositoriesResult;
    do {
      describeRepositoriesResult = awsApiHelperService.listRepositories(awsConfig, describeRepositoriesRequest, region);
      describeRepositoriesResult.getRepositories().forEach(repository -> repoNames.add(repository.getRepositoryName()));
      describeRepositoriesRequest.setNextToken(describeRepositoriesResult.getNextToken());
    } while (describeRepositoriesRequest.getNextToken() != null);

    return repoNames;
  }

  @Override
  public List<Map<String, String>> getLabels(
      AwsInternalConfig awsConfig, String imageName, String region, List<String> tags) {
    return Collections.singletonList(awsApiHelperService.fetchLabels(awsConfig, imageName, region, tags));
  }
}
