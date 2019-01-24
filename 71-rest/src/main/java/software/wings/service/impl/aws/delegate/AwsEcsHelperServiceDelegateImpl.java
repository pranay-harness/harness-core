package software.wings.service.impl.aws.delegate;

import static com.amazonaws.services.ecs.model.ServiceField.TAGS;
import static com.google.common.collect.Lists.newArrayList;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.DescribeServicesRequest;
import com.amazonaws.services.ecs.model.DescribeServicesResult;
import com.amazonaws.services.ecs.model.ListClustersRequest;
import com.amazonaws.services.ecs.model.ListClustersResult;
import com.amazonaws.services.ecs.model.ListServicesRequest;
import com.amazonaws.services.ecs.model.ListServicesResult;
import com.amazonaws.services.ecs.model.Service;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.aws.delegate.AwsEcsHelperServiceDelegate;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class AwsEcsHelperServiceDelegateImpl
    extends AwsHelperServiceDelegateBase implements AwsEcsHelperServiceDelegate {
  @VisibleForTesting
  AmazonECSClient getAmazonEcsClient(String region, String accessKey, char[] secretKey, boolean useEc2IamCredentials) {
    AmazonECSClientBuilder builder = AmazonECSClientBuilder.standard().withRegion(region);
    attachCredentials(builder, useEc2IamCredentials, accessKey, secretKey);
    return (AmazonECSClient) builder.build();
  }

  private String getIdFromArn(String arn) {
    return arn.substring(arn.lastIndexOf('/') + 1);
  }

  @Override
  public List<String> listClusters(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      List<String> result = new ArrayList<>();
      String nextToken = null;
      do {
        ListClustersRequest listClustersRequest = new ListClustersRequest().withNextToken(nextToken);
        ListClustersResult listClustersResult = getAmazonEcsClient(
            region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials())
                                                    .listClusters(listClustersRequest);
        result.addAll(listClustersResult.getClusterArns().stream().map(this ::getIdFromArn).collect(toList()));
        nextToken = listClustersResult.getNextToken();
      } while (nextToken != null);
      return result;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  @Override
  public List<Service> listServicesForCluster(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String cluster) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonECSClient client = getAmazonEcsClient(
          region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials());
      List<String> serviceArns = newArrayList();
      String nextToken = null;
      do {
        ListServicesRequest listServicesRequest =
            new ListServicesRequest().withCluster(cluster).withNextToken(nextToken);
        ListServicesResult listServicesResult = client.listServices(listServicesRequest);
        List<String> arnsBatch = listServicesResult.getServiceArns();
        if (isNotEmpty(arnsBatch)) {
          serviceArns.addAll(arnsBatch);
        }
        nextToken = listServicesResult.getNextToken();
      } while (nextToken != null);
      int counter = 0;
      List<Service> allServices = newArrayList();
      while (counter < serviceArns.size()) {
        // We can ONLY describe 10 services at a time.
        List<String> arnsBatch = newArrayList();
        for (int i = 0; i < 10 && counter < serviceArns.size(); i++) {
          arnsBatch.add(serviceArns.get(counter));
          counter++;
        }
        DescribeServicesRequest describeServicesRequest =
            new DescribeServicesRequest().withCluster(cluster).withServices(arnsBatch).withInclude(TAGS);
        DescribeServicesResult describeServicesResult = client.describeServices(describeServicesRequest);
        allServices.addAll(describeServicesResult.getServices());
      }
      return allServices;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }
}