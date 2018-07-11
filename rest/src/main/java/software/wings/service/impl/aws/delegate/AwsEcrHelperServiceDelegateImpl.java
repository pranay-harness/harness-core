package software.wings.service.impl.aws.delegate;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.singletonList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ecr.AmazonECRClient;
import com.amazonaws.services.ecr.AmazonECRClientBuilder;
import com.amazonaws.services.ecr.model.DescribeRepositoriesRequest;
import com.amazonaws.services.ecr.model.DescribeRepositoriesResult;
import com.amazonaws.services.ecr.model.GetAuthorizationTokenRequest;
import com.amazonaws.services.ecr.model.Repository;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.aws.delegate.AwsEcrHelperServiceDelegate;

import java.util.List;

@Singleton
public class AwsEcrHelperServiceDelegateImpl
    extends AwsHelperServiceDelegateBase implements AwsEcrHelperServiceDelegate {
  @VisibleForTesting
  AmazonECRClient getAmazonEcrClient(AwsConfig awsConfig, String region) {
    return (AmazonECRClient) AmazonECRClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(awsConfig.getAccessKey(), new String(awsConfig.getSecretKey()))))
        .build();
  }

  private DescribeRepositoriesResult listRepositories(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      DescribeRepositoriesRequest describeRepositoriesRequest, String region) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcrClient(awsConfig, region).describeRepositories(describeRepositoriesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeRepositoriesResult();
  }

  private Repository getRepository(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String repositoryName) {
    DescribeRepositoriesRequest describeRepositoriesRequest = new DescribeRepositoriesRequest();
    describeRepositoriesRequest.setRepositoryNames(Lists.newArrayList(repositoryName));
    DescribeRepositoriesResult describeRepositoriesResult =
        listRepositories(awsConfig, encryptionDetails, describeRepositoriesRequest, region);
    List<Repository> repositories = describeRepositoriesResult.getRepositories();
    if (isNotEmpty(repositories)) {
      return repositories.get(0);
    }
    return null;
  }

  @Override
  public String getEcrImageUrl(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String imageName) {
    Repository repository = getRepository(awsConfig, encryptionDetails, region, imageName);
    return repository != null ? repository.getRepositoryUri() : null;
  }

  @Override
  public String getAmazonEcrAuthToken(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String awsAccount, String region) {
    encryptionService.decrypt(awsConfig, encryptionDetails);
    AmazonECRClient ecrClient = getAmazonEcrClient(awsConfig, region);
    return ecrClient
        .getAuthorizationToken(new GetAuthorizationTokenRequest().withRegistryIds(singletonList(awsAccount)))
        .getAuthorizationData()
        .get(0)
        .getAuthorizationToken();
  }
}