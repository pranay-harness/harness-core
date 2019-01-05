package software.wings.service.impl.aws.delegate;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.ListClustersRequest;
import com.amazonaws.services.ecs.model.ListClustersResult;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import com.amazonaws.services.elasticloadbalancingv2.model.ModifyListenerRequest;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.aws.delegate.AwsEcsHelperServiceDelegate;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class AwsEcsHelperServiceDelegateImpl
    extends AwsHelperServiceDelegateBase implements AwsEcsHelperServiceDelegate {
  @Inject AwsHelperService awsHelperService;

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

  @VisibleForTesting
  AmazonElasticLoadBalancing getAmazonElbV2Client(
      String region, String accessKey, char[] secretKey, boolean useEc2IamCredentials) {
    AmazonElasticLoadBalancingClientBuilder builder = AmazonElasticLoadBalancingClient.builder().withRegion(region);
    attachCredentials(builder, useEc2IamCredentials, accessKey, secretKey);
    return (AmazonElasticLoadBalancing) builder.build();
  }

  @Override
  public void updateListenersForEcsBG(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String prodListenerArn, String stageListenerArn, String region) {
    encryptionService.decrypt(awsConfig, encryptionDetails);

    AmazonElasticLoadBalancing client = getAmazonElbV2Client(
        region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials());

    DescribeListenersResult prodListenerResult =
        client.describeListeners(new DescribeListenersRequest().withListenerArns(prodListenerArn));

    DescribeListenersResult stageListenerResult =
        client.describeListeners(new DescribeListenersRequest().withListenerArns(stageListenerArn));

    Listener prodListener = prodListenerResult.getListeners().get(0);
    Listener stageListener = stageListenerResult.getListeners().get(0);

    client.modifyListener(new ModifyListenerRequest()
                              .withListenerArn(prodListener.getListenerArn())
                              .withDefaultActions(stageListener.getDefaultActions()));

    client.modifyListener(new ModifyListenerRequest()
                              .withListenerArn(stageListener.getListenerArn())
                              .withDefaultActions(prodListener.getDefaultActions()));
  }

  @Override
  public DescribeListenersResult describeListenerResult(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String listenerArn, String region) {
    AmazonElasticLoadBalancing client = getAmazonElbV2Client(
        region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials());

    return client.describeListeners(new DescribeListenersRequest().withListenerArns(listenerArn));
  }
}