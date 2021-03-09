package software.wings.helpers.ext.ecr;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.beans.BuildDetailsInternal.BuildDetailsInternalMetadataKeys;
import io.harness.artifacts.comparator.BuildDetailsInternalComparatorAscending;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.GeneralException;

import software.wings.service.impl.AwsApiHelperService;

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
  @Inject private AwsApiHelperService awsApiHelperService;

  @Override
  public List<BuildDetailsInternal> getBuilds(
      AwsInternalConfig awsConfig, String imageUrl, String region, String imageName, int maxNumberOfBuilds) {
    List<BuildDetailsInternal> buildDetailsInternals = new ArrayList<>();
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
              metadata.put(BuildDetailsInternalMetadataKeys.image, imageUrl + ":" + imageIdentifier.getImageTag());
              metadata.put(BuildDetailsInternalMetadataKeys.tag, imageIdentifier.getImageTag());
              buildDetailsInternals.add(BuildDetailsInternal.builder()
                                            .number(imageIdentifier.getImageTag())
                                            .metadata(metadata)
                                            .uiDisplayName("Tag# " + imageIdentifier.getImageTag())
                                            .build());
            });
        listImagesRequest.setNextToken(listImagesResult.getNextToken());
      } while (listImagesRequest.getNextToken() != null);
    } catch (Exception e) {
      throw new GeneralException(ExceptionUtils.getMessage(e), USER);
    }
    // Sorting at build tag for docker artifacts.
    return buildDetailsInternals.stream().sorted(new BuildDetailsInternalComparatorAscending()).collect(toList());
  }

  @Override
  public BuildDetailsInternal getLastSuccessfulBuild(AwsInternalConfig awsConfig, String imageName) {
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
