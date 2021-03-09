package software.wings.service.impl.delegate;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.artifacts.ecr.beans.AwsInternalConfig;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ecr.AmazonECRClient;
import com.amazonaws.services.ecr.AmazonECRClientBuilder;
import com.amazonaws.services.ecr.model.DescribeRepositoriesRequest;
import com.amazonaws.services.ecr.model.DescribeRepositoriesResult;
import com.amazonaws.services.ecr.model.Repository;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
public class AwsEcrApiHelperServiceDelegateImpl extends AwsEcrApiHelperServiceDelegateBase {
  private AmazonECRClient getAmazonEcrClient(AwsInternalConfig awsConfig, String region) {
    AmazonECRClientBuilder builder = AmazonECRClientBuilder.standard().withRegion(region);
    attachCredentialsAndBackoffPolicy(builder, awsConfig);
    return (AmazonECRClient) builder.build();
  }
  private DescribeRepositoriesResult listRepositories(
      AwsInternalConfig awsConfig, DescribeRepositoriesRequest describeRepositoriesRequest, String region) {
    try {
      tracker.trackECRCall("List Repositories");
      return getAmazonEcrClient(awsConfig, region).describeRepositories(describeRepositoriesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new DescribeRepositoriesResult();
  }
  private Repository getRepository(AwsInternalConfig awsConfig, String region, String repositoryName) {
    DescribeRepositoriesRequest describeRepositoriesRequest = new DescribeRepositoriesRequest();
    describeRepositoriesRequest.setRepositoryNames(Lists.newArrayList(repositoryName));
    DescribeRepositoriesResult describeRepositoriesResult =
        listRepositories(awsConfig, describeRepositoriesRequest, region);
    List<Repository> repositories = describeRepositoriesResult.getRepositories();
    if (isNotEmpty(repositories)) {
      return repositories.get(0);
    }
    return null;
  }

  public String getEcrImageUrl(AwsInternalConfig awsConfig, String region, String imageName) {
    Repository repository = getRepository(awsConfig, region, imageName);
    return repository != null ? repository.getRepositoryUri() : null;
  }
}
