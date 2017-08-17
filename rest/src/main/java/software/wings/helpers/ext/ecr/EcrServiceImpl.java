package software.wings.helpers.ext.ecr;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ecr.model.DescribeRepositoriesRequest;
import com.amazonaws.services.ecr.model.DescribeRepositoriesResult;
import com.amazonaws.services.ecr.model.ListImagesRequest;
import com.amazonaws.services.ecr.model.ListImagesResult;
import com.amazonaws.services.ecr.model.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.EcrConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.AwsHelperService;
import software.wings.settings.SettingValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by brett on 7/15/17
 */
@Singleton
public class EcrServiceImpl implements EcrService {
  @Inject private AwsHelperService awsHelperService;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public List<BuildDetails> getBuilds(AwsConfig awsConfig, String region, String imageName, int maxNumberOfBuilds) {
    List<BuildDetails> buildDetails = new ArrayList<>();
    ListImagesResult listImagesResult;
    ListImagesRequest listImagesRequest = new ListImagesRequest().withRepositoryName(imageName);
    do {
      listImagesResult = awsHelperService.listEcrImages(awsConfig, region, listImagesRequest);
      listImagesResult.getImageIds()
          .stream()
          .filter(imageIdentifier
              -> imageIdentifier != null && imageIdentifier.getImageTag() != null
                  && !imageIdentifier.getImageTag().isEmpty())
          .forEach(imageIdentifier
              -> buildDetails.add(
                  BuildDetails.Builder.aBuildDetails().withNumber(imageIdentifier.getImageTag()).build()));
      listImagesRequest.setNextToken(listImagesResult.getNextToken());
    } while (listImagesRequest.getNextToken() != null);
    return buildDetails;
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(AwsConfig awsConfig, String imageName) {
    return null;
  }

  @Override
  public boolean verifyRepository(AwsConfig awsConfig, String region, String repositoryName) {
    return listEcrRegistry(awsConfig, region).contains(repositoryName);
  }

  public List<String> listRegions(AwsConfig awsConfig) {
    return awsHelperService.listRegions(awsConfig);
  }

  @Override
  public List<String> listEcrRegistry(AwsConfig awsConfig, String region) {
    List<String> repoNames = new ArrayList<>();
    DescribeRepositoriesRequest describeRepositoriesRequest = new DescribeRepositoriesRequest();
    DescribeRepositoriesResult describeRepositoriesResult;
    do {
      describeRepositoriesResult = awsHelperService.listRepositories(awsConfig, describeRepositoriesRequest, region);
      describeRepositoriesResult.getRepositories().forEach(repository -> repoNames.add(repository.getRepositoryName()));
      describeRepositoriesRequest.setNextToken(describeRepositoriesResult.getNextToken());
    } while (describeRepositoriesRequest.getNextToken() != null);

    return repoNames;
  }

  @Override
  public String getEcrImageUrl(AwsConfig awsConfig, String region, EcrArtifactStream ecrArtifactStream) {
    Repository repository = awsHelperService.getRepository(awsConfig, region, ecrArtifactStream.getImageName());
    if (repository != null) {
      String imageUrl = repository.getRepositoryUri();
      return imageUrl;
    }
    return null;
  }
}
