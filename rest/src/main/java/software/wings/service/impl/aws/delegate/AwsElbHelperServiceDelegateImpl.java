package software.wings.service.impl.aws.delegate;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsResult;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.aws.delegate.AwsElbHelperServiceDelegate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class AwsElbHelperServiceDelegateImpl
    extends AwsHelperServiceDelegateBase implements AwsElbHelperServiceDelegate {
  @VisibleForTesting
  com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient getClassicElbClient(
      Regions region, String accessKey, char[] secretKey) {
    return (com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient)
        AmazonElasticLoadBalancingClientBuilder.standard()
            .withRegion(region)
            .withCredentials(
                new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
            .build();
  }

  @VisibleForTesting
  AmazonElasticLoadBalancingClient getAmazonElasticLoadBalancingClientV2(
      Regions region, String accessKey, char[] secretKey) {
    return (AmazonElasticLoadBalancingClient) com.amazonaws.services.elasticloadbalancingv2
        .AmazonElasticLoadBalancingClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
  }

  @Override
  public List<String> listClassicLoadBalancers(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      List<String> result = new ArrayList<>();
      String nextMarker = null;
      do {
        com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest describeLoadBalancersRequest =
            new com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest()
                .withPageSize(400)
                .withMarker(nextMarker);
        com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult describeLoadBalancersResult =
            getClassicElbClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
                .describeLoadBalancers(describeLoadBalancersRequest);
        result.addAll(describeLoadBalancersResult.getLoadBalancerDescriptions()
                          .stream()
                          .map(LoadBalancerDescription::getLoadBalancerName)
                          .collect(toList()));
        nextMarker = describeLoadBalancersResult.getNextMarker();
      } while (nextMarker != null);
      return result;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  @Override
  public List<String> listApplicationLoadBalancers(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      List<String> result = new ArrayList<>();
      String nextMarker = null;
      do {
        AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient = getAmazonElasticLoadBalancingClientV2(
            Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
        DescribeLoadBalancersRequest describeLoadBalancersRequest =
            new DescribeLoadBalancersRequest().withMarker(nextMarker).withPageSize(400);
        DescribeLoadBalancersResult describeLoadBalancersResult =
            amazonElasticLoadBalancingClient.describeLoadBalancers(describeLoadBalancersRequest);
        result.addAll(describeLoadBalancersResult.getLoadBalancers()
                          .stream()
                          .filter(loadBalancer -> StringUtils.equalsIgnoreCase(loadBalancer.getType(), "application"))
                          .map(LoadBalancer::getLoadBalancerName)
                          .collect(toList()));
        nextMarker = describeLoadBalancersResult.getNextMarker();
      } while (nextMarker != null);
      return result;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  @Override
  public Map<String, String> listTargetGroupsForAlb(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String loadBalancerName) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient = getAmazonElasticLoadBalancingClientV2(
          Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      String loadBalancerArn = null;
      if (isNotBlank(loadBalancerName)) {
        DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest();
        request.withNames(loadBalancerName);
        loadBalancerArn = amazonElasticLoadBalancingClient.describeLoadBalancers(request)
                              .getLoadBalancers()
                              .get(0)
                              .getLoadBalancerArn();
      }
      Map<String, String> result = new HashMap<>();
      String nextMarker = null;
      do {
        DescribeTargetGroupsRequest describeTargetGroupsRequest =
            new DescribeTargetGroupsRequest().withMarker(nextMarker).withPageSize(400);
        if (loadBalancerArn != null) {
          describeTargetGroupsRequest.withLoadBalancerArn(loadBalancerArn);
        }
        DescribeTargetGroupsResult describeTargetGroupsResult =
            amazonElasticLoadBalancingClient.describeTargetGroups(describeTargetGroupsRequest);
        describeTargetGroupsResult.getTargetGroups().forEach(
            group -> result.put(group.getTargetGroupArn(), group.getTargetGroupName()));
        nextMarker = describeTargetGroupsResult.getNextMarker();
      } while (nextMarker != null);
      return result;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyMap();
  }
}