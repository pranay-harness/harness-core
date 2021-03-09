package software.wings.helpers.ext.ecr;

import com.amazonaws.services.ecr.model.DescribeRepositoriesRequest;
import com.amazonaws.services.ecr.model.DescribeRepositoriesResult;
import com.amazonaws.services.ecr.model.ListImagesRequest;
import com.amazonaws.services.ecr.model.ListImagesResult;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.annotations.dev.OwnedBy;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.beans.BuildDetailsInternal.BuildDetailsInternalMetadataKeys;
import io.harness.artifacts.comparator.BuildDetailsInternalComparatorAscending;
import io.harness.artifacts.comparator.BuildDetailsInternalComparatorDescending;
import io.harness.artifacts.ecr.beans.AwsInternalConfig;
import io.harness.exception.ArtifactServerException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidArtifactServerException;
import io.harness.expression.RegexFunctor;
import software.wings.service.impl.AwsApiHelperService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.util.stream.Collectors.toList;

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

  @Override
  public BuildDetailsInternal getLastSuccessfulBuildFromRegex(
      AwsInternalConfig awsInternalConfig, String imageUrl, String region, String imageName, String tagRegex) {
    List<BuildDetailsInternal> builds =
        getBuilds(awsInternalConfig, imageUrl, region, imageName, MAX_NO_OF_TAGS_PER_IMAGE);
    builds = builds.stream()
                 .filter(build -> new RegexFunctor().match(tagRegex, build.getNumber()))
                 .sorted(new BuildDetailsInternalComparatorDescending())
                 .collect(toList());
    if (builds.isEmpty()) {
      throw new InvalidArtifactServerException(
          "There are no builds for this image: " + imageName + " and tagRegex: " + tagRegex, USER);
    }
    return builds.get(0);
  }

  // Todo: to be implemented
  @Override
  public boolean validateCredentials(AwsInternalConfig awsConfig, String imageName) {
    return true;
  }

  @Override
  public BuildDetailsInternal verifyBuildNumber(
      AwsInternalConfig awsInternalConfig, String imageUrl, String region, String imageName, String tag) {
    try {
      List<BuildDetailsInternal> builds =
          getBuilds(awsInternalConfig, imageUrl, region, imageName, MAX_NO_OF_TAGS_PER_IMAGE);
      builds = builds.stream().filter(build -> build.getNumber().equals(tag)).collect(Collectors.toList());
      if (builds.size() != 1) {
        throw new InvalidArtifactServerException("Didn't get build number", USER);
      }
      return builds.get(0);
    } catch (Exception e) {
      throw new ArtifactServerException(ExceptionUtils.getMessage(e), e, USER);
    }
  }

  /**
   * The type ECR image tag response.
   */
  public static class EcrImageTagResponse {
    private List<String> child;
    private String name;
    private List<String> tags;
    private Map manifest;

    public Map getManifest() {
      return manifest;
    }

    public void setManifest(Map manifest) {
      this.manifest = manifest;
    }

    public List<String> getChild() {
      return child;
    }

    public void setChild(List<String> child) {
      this.child = child;
    }

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
      return name;
    }

    /**
     * Sets name.
     *
     * @param name the name
     */
    public void setName(String name) {
      this.name = name;
    }

    /**
     * Gets tags.
     *
     * @return the tags
     */
    public List<String> getTags() {
      return tags;
    }

    /**
     * Sets tags.
     *
     * @param tags the tags
     */
    public void setTags(List<String> tags) {
      this.tags = tags;
    }
  }
}
