package software.wings.helpers.ext.ecr;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.exception.WingsException.USER;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;
import static software.wings.helpers.ext.jenkins.BuildDetails.METADATA_IMAGE_KEY;
import static software.wings.helpers.ext.jenkins.BuildDetails.METADATA_TAG_KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ecr.model.DescribeRepositoriesRequest;
import com.amazonaws.services.ecr.model.DescribeRepositoriesResult;
import com.amazonaws.services.ecr.model.ListImagesRequest;
import com.amazonaws.services.ecr.model.ListImagesResult;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import software.wings.beans.AwsConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.aws.delegate.AwsEcrHelperServiceDelegate;
import software.wings.service.intfc.security.EncryptionService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by brett on 7/15/17
 */
@Singleton
public class EcrServiceImpl implements EcrService {
  @Inject private AwsHelperService awsHelperService;
  @Inject private EncryptionService encryptionService;
  @Inject private AwsEcrHelperServiceDelegate ecrServiceDelegate;

  @Override
  public List<BuildDetails> getBuilds(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String imageName, int maxNumberOfBuilds) {
    List<BuildDetails> buildDetails = new ArrayList<>();
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      String imageUrl = ecrServiceDelegate.getEcrImageUrl(awsConfig, encryptionDetails, region, imageName);

      ListImagesResult listImagesResult;
      ListImagesRequest listImagesRequest = new ListImagesRequest().withRepositoryName(imageName);
      do {
        listImagesResult = awsHelperService.listEcrImages(awsConfig, encryptionDetails, region, listImagesRequest);
        listImagesResult.getImageIds()
            .stream()
            .filter(imageIdentifier -> imageIdentifier != null && isNotEmpty(imageIdentifier.getImageTag()))
            .forEach(imageIdentifier -> {
              Map<String, String> metadata = new HashMap();
              metadata.put(METADATA_IMAGE_KEY, imageUrl + ":" + imageIdentifier.getImageTag());
              metadata.put(METADATA_TAG_KEY, imageIdentifier.getImageTag());
              buildDetails.add(
                  aBuildDetails().withNumber(imageIdentifier.getImageTag()).withMetadata(metadata).build());
            });
        listImagesRequest.setNextToken(listImagesResult.getNextToken());
      } while (listImagesRequest.getNextToken() != null);
    } catch (Exception e) {
      throw new WingsException(GENERAL_ERROR, USER).addParam("message", ExceptionUtils.getMessage(e));
    }
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
}
