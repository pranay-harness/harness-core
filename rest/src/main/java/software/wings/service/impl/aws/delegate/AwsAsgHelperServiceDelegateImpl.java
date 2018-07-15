package software.wings.service.impl.aws.delegate;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.ec2.model.Instance;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.aws.delegate.AwsAsgHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class AwsAsgHelperServiceDelegateImpl
    extends AwsHelperServiceDelegateBase implements AwsAsgHelperServiceDelegate {
  @Inject private AwsEc2HelperServiceDelegate awsEc2HelperServiceDelegate;

  @VisibleForTesting
  AmazonAutoScalingClient getAmazonAutoScalingClient(Regions region, String accessKey, char[] secretKey) {
    return (AmazonAutoScalingClient) AmazonAutoScalingClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
  }

  @Override
  public List<String> listAutoScalingGroupNames(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonAutoScalingClient amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult =
          amazonAutoScalingClient.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withMaxRecords(100));
      List<AutoScalingGroup> result = new ArrayList<>(describeAutoScalingGroupsResult.getAutoScalingGroups());
      while (isNotEmpty(describeAutoScalingGroupsResult.getNextToken())) {
        describeAutoScalingGroupsResult = amazonAutoScalingClient.describeAutoScalingGroups(
            new DescribeAutoScalingGroupsRequest().withMaxRecords(100).withNextToken(
                describeAutoScalingGroupsResult.getNextToken()));
        result.addAll(describeAutoScalingGroupsResult.getAutoScalingGroups());
      }
      return result.stream().map(AutoScalingGroup::getAutoScalingGroupName).collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  @Override
  public List<Instance> listAutoScalingGroupInstances(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      DescribeAutoScalingGroupsRequest describeAutoScalingGroupsRequest =
          new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(autoScalingGroupName);
      AmazonAutoScalingClient amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult =
          amazonAutoScalingClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest);
      if (describeAutoScalingGroupsResult.getAutoScalingGroups().isEmpty()) {
        return emptyList();
      }
      AutoScalingGroup autoScalingGroup = describeAutoScalingGroupsResult.getAutoScalingGroups().get(0);
      List<String> instanceIds = autoScalingGroup.getInstances()
                                     .stream()
                                     .map(com.amazonaws.services.autoscaling.model.Instance::getInstanceId)
                                     .collect(toList());
      return awsEc2HelperServiceDelegate.listEc2Instances(awsConfig, encryptionDetails, instanceIds, region);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }
}