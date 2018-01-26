package software.wings.helpers.ext.ecr;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

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
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.security.EncryptionService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by brett on 7/15/17
 */
@Singleton
public class EcrClassicServiceImpl implements EcrClassicService {
  @Inject private AwsHelperService awsHelperService;
  @Inject private EncryptionService encryptionService;
  private static final Logger logger = LoggerFactory.getLogger(EcrClassicServiceImpl.class);

  @Override
  public List<BuildDetails> getBuilds(
      EcrConfig ecrConfig, List<EncryptedDataDetail> encryptionDetails, String imageName, int maxNumberOfBuilds) {
    List<BuildDetails> buildDetails = new ArrayList<>();
    ListImagesResult listImagesResult;
    ListImagesRequest listImagesRequest = new ListImagesRequest().withRepositoryName(imageName);
    do {
      listImagesResult = awsHelperService.listEcrImages(ecrConfig, encryptionDetails, listImagesRequest);
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
      EcrConfig ecrConfig, List<EncryptedDataDetail> encryptionDetails, String imageName) {
    return null;
  }

  @Override
  public boolean verifyRepository(
      EcrConfig ecrConfig, List<EncryptedDataDetail> encryptionDetails, String repositroyName) {
    return listEcrRegistry(ecrConfig, encryptionDetails).contains(repositroyName);
  }

  @Override
  public boolean validateCredentials(EcrConfig ecrConfig, List<EncryptedDataDetail> encryptionDetails) {
    encryptionService.decrypt(ecrConfig, encryptionDetails);
    awsHelperService.validateAwsAccountCredential(ecrConfig.getAccessKey(), ecrConfig.getSecretKey());
    return true;
  }

  @Override
  public List<String> listEcrRegistry(EcrConfig ecrConfig, List<EncryptedDataDetail> encryptionDetails) {
    List<String> repoNames = new ArrayList<>();
    DescribeRepositoriesRequest describeRepositoriesRequest = new DescribeRepositoriesRequest();
    DescribeRepositoriesResult describeRepositoriesResult;
    do {
      describeRepositoriesResult =
          awsHelperService.listRepositories(ecrConfig, encryptionDetails, describeRepositoriesRequest);
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
