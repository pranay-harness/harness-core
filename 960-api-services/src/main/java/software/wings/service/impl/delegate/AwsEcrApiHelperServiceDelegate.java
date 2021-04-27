package software.wings.service.impl.delegate;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.util.Collections.singletonList;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.globalcontex.ErrorHandlingGlobalContextData;
import io.harness.manage.GlobalContextManager;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ecr.AmazonECRClient;
import com.amazonaws.services.ecr.AmazonECRClientBuilder;
import com.amazonaws.services.ecr.model.DescribeRepositoriesRequest;
import com.amazonaws.services.ecr.model.DescribeRepositoriesResult;
import com.amazonaws.services.ecr.model.GetAuthorizationTokenRequest;
import com.amazonaws.services.ecr.model.Repository;
import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import java.util.List;

@Singleton
@OwnedBy(HarnessTeam.PIPELINE)
public class AwsEcrApiHelperServiceDelegate extends AwsEcrApiHelperServiceDelegateBase {
  public AmazonECRClient getAmazonEcrClient(AwsInternalConfig awsConfig, String region) {
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
      ErrorHandlingGlobalContextData globalContextData =
          GlobalContextManager.get(ErrorHandlingGlobalContextData.IS_SUPPORTED_ERROR_FRAMEWORK);
      if (globalContextData.isSupportedErrorFramework()) {
        GlobalContextManager.unset(ErrorHandlingGlobalContextData.IS_SUPPORTED_ERROR_FRAMEWORK);
        throw amazonServiceException;
      }
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      ErrorHandlingGlobalContextData globalContextData =
          GlobalContextManager.get(ErrorHandlingGlobalContextData.IS_SUPPORTED_ERROR_FRAMEWORK);
      if (globalContextData.isSupportedErrorFramework()) {
        GlobalContextManager.unset(ErrorHandlingGlobalContextData.IS_SUPPORTED_ERROR_FRAMEWORK);
        throw amazonClientException;
      }
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
  public String getAmazonEcrAuthToken(AwsInternalConfig awsConfig, String awsAccount, String region) {
    AmazonECRClient ecrClient = getAmazonEcrClient(awsConfig, region);
    tracker.trackECRCall("Get Ecr Auth Token");
    return ecrClient
        .getAuthorizationToken(new GetAuthorizationTokenRequest().withRegistryIds(singletonList(awsAccount)))
        .getAuthorizationData()
        .get(0)
        .getAuthorizationToken();
  }
}
