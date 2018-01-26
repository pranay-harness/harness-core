package software.wings.helpers.ext.ecr;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

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
import software.wings.beans.artifact.EcrArtifactStream;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.security.EncryptionService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by brett on 7/15/17
 */
@Singleton
public class EcrServiceImpl implements EcrService {
  @Inject private AwsHelperService awsHelperService;
  @Inject private EncryptionService encryptionService;
  private static final Logger logger = LoggerFactory.getLogger(EcrServiceImpl.class);

  @Override
  public List<BuildDetails> getBuilds(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String imageName, int maxNumberOfBuilds) {
    List<BuildDetails> buildDetails = new ArrayList<>();
    ListImagesResult listImagesResult;
    ListImagesRequest listImagesRequest = new ListImagesRequest().withRepositoryName(imageName);
    do {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      listImagesResult = awsHelperService.listEcrImages(awsConfig, encryptionDetails, region, listImagesRequest);
      listImagesResult.getImageIds()
          .stream()
          .filter(imageIdentifier -> imageIdentifier != null && isNotEmpty(imageIdentifier.getImageTag()))
          .forEach(imageIdentifier
              -> buildDetails.add(
                  BuildDetails.Builder.aBuildDetails().withNumber(imageIdentifier.getImageTag()).build()));
      listImagesRequest.setNextToken(listImagesResult.getNextToken());
    } while (listImagesRequest.getNextToken() != null);
    return buildDetails;
  }

  @Override
  public BuildDetails getLastSuccessfulBuild(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String imageName) {
    return null;
  }

  @Override
  public boolean verifyRepository(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String repositoryName) {
    return listEcrRegistry(awsConfig, encryptionDetails, region).contains(repositoryName);
  }

  public List<String> listRegions(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    return awsHelperService.listRegions(awsConfig, encryptionDetails);
  }

  @Override
  public List<String> listEcrRegistry(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    List<String> repoNames = new ArrayList<>();
    DescribeRepositoriesRequest describeRepositoriesRequest = new DescribeRepositoriesRequest();
    DescribeRepositoriesResult describeRepositoriesResult;
    do {
      describeRepositoriesResult =
          awsHelperService.listRepositories(awsConfig, encryptionDetails, describeRepositoriesRequest, region);
      describeRepositoriesResult.getRepositories().forEach(repository -> repoNames.add(repository.getRepositoryName()));
      describeRepositoriesRequest.setNextToken(describeRepositoriesResult.getNextToken());
    } while (describeRepositoriesRequest.getNextToken() != null);

    return repoNames;
  }

  @Override
  public String getEcrImageUrl(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      EcrArtifactStream ecrArtifactStream) {
    Repository repository =
        awsHelperService.getRepository(awsConfig, encryptionDetails, region, ecrArtifactStream.getImageName());
    if (repository != null) {
      return repository.getRepositoryUri();
    }
    return null;
  }
}
