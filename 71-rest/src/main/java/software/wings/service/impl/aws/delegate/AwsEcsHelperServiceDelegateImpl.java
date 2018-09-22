package software.wings.service.impl.aws.delegate;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.ListClustersRequest;
import com.amazonaws.services.ecs.model.ListClustersResult;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.aws.delegate.AwsEcsHelperServiceDelegate;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class AwsEcsHelperServiceDelegateImpl
    extends AwsHelperServiceDelegateBase implements AwsEcsHelperServiceDelegate {
  @VisibleForTesting
  AmazonECSClient getAmazonEcsClient(String region, String accessKey, char[] secretKey) {
    return (AmazonECSClient) AmazonECSClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
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
        ListClustersResult listClustersResult =
            getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
                .listClusters(listClustersRequest);
        result.addAll(listClustersResult.getClusterArns().stream().map(this ::getIdFromArn).collect(toList()));
        nextToken = listClustersResult.getNextToken();
      } while (nextToken != null);
      return result;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }
}