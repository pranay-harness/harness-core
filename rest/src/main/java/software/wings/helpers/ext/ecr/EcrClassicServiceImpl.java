package software.wings.helpers.ext.ecr;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ecr.model.DescribeRepositoriesRequest;
import com.amazonaws.services.ecr.model.DescribeRepositoriesResult;
import com.amazonaws.services.ecr.model.ListImagesRequest;
import com.amazonaws.services.ecr.model.ListImagesResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.EcrConfig;
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.AwsHelperService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by brett on 7/15/17
 */
@Singleton
public class EcrClassicServiceImpl implements EcrClassicService {
  @Inject private AwsHelperService awsHelperService;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Override
  public List<BuildDetails> getBuilds(EcrConfig ecrConfig, String imageName, int maxNumberOfBuilds) {
    List<BuildDetails> buildDetails = new ArrayList<>();
    ListImagesResult listImagesResult;
    ListImagesRequest listImagesRequest = new ListImagesRequest().withRepositoryName(imageName);
    do {
      listImagesResult = awsHelperService.listEcrImages(ecrConfig, listImagesRequest);
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
  public BuildDetails getLastSuccessfulBuild(EcrConfig ecrConfig, String imageName) {
    return null;
  }

  @Override
  public boolean verifyRepository(EcrConfig ecrConfig, String repositroyName) {
    return listEcrRegistry(ecrConfig).contains(repositroyName);
  }

  @Override
  public boolean validateCredentials(EcrConfig ecrConfig) {
    awsHelperService.validateAwsAccountCredential(ecrConfig.getAccessKey(), ecrConfig.getSecretKey());
    return true;
  }

  @Override
  public List<String> listEcrRegistry(EcrConfig ecrConfig) {
    List<String> repoNames = new ArrayList<>();
    DescribeRepositoriesRequest describeRepositoriesRequest = new DescribeRepositoriesRequest();
    DescribeRepositoriesResult describeRepositoriesResult;
    do {
      describeRepositoriesResult = awsHelperService.listRepositories(ecrConfig, describeRepositoriesRequest);
      describeRepositoriesResult.getRepositories().forEach(repository -> repoNames.add(repository.getRepositoryName()));
      describeRepositoriesRequest.setNextToken(describeRepositoriesResult.getNextToken());
    } while (describeRepositoriesRequest.getNextToken() != null);

    return repoNames;
  }

  @Override
  public String getEcrImageUrl(EcrConfig ecrConfig, EcrArtifactStream ecrArtifactStream) {
    String registry = ecrConfig.getEcrUrl().substring(8);
    if (!registry.endsWith("/")) {
      registry += "/";
    }

    return registry + ecrArtifactStream.getImageName();
  }
}
