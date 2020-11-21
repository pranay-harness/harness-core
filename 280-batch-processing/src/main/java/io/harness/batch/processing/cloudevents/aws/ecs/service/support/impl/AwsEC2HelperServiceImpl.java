package io.harness.batch.processing.cloudevents.aws.ecs.service.support.impl;

import static java.util.Collections.emptyList;

import io.harness.batch.processing.cloudevents.aws.ecs.service.support.AwsCredentialHelper;
import io.harness.batch.processing.cloudevents.aws.ecs.service.support.intfc.AwsEC2HelperService;

import software.wings.beans.AwsCrossAccountAttributes;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AwsEC2HelperServiceImpl implements AwsEC2HelperService {
  @Autowired private AwsCredentialHelper awsCredentialHelper;

  @Override
  public List<Instance> listEc2Instances(
      AwsCrossAccountAttributes awsCrossAccountAttributes, Set<String> instanceIds, String region) {
    try {
      AmazonEC2Client amazonEC2Client = getAmazonEC2Client(region, awsCrossAccountAttributes);
      List<Instance> ec2Instances = new ArrayList<>();
      DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest().withInstanceIds(instanceIds);
      String nextToken = null;
      do {
        describeInstancesRequest.withNextToken(nextToken);
        DescribeInstancesResult describeInstancesResult = amazonEC2Client.describeInstances(describeInstancesRequest);
        ec2Instances.addAll(getInstanceList(describeInstancesResult));
        nextToken = describeInstancesResult.getNextToken();
      } while (nextToken != null);
      return ec2Instances;
    } catch (Exception ex) {
      log.error("Exception listEc2Instances ", ex);
    }
    return emptyList();
  }

  private List<Instance> getInstanceList(DescribeInstancesResult result) {
    List<Instance> instanceList = Lists.newArrayList();
    result.getReservations().forEach(reservation -> instanceList.addAll(reservation.getInstances()));
    return instanceList;
  }

  @VisibleForTesting
  AmazonEC2Client getAmazonEC2Client(String region, AwsCrossAccountAttributes awsCrossAccountAttributes) {
    AWSSecurityTokenService awsSecurityTokenService = awsCredentialHelper.constructAWSSecurityTokenService();
    AmazonEC2ClientBuilder builder = AmazonEC2ClientBuilder.standard().withRegion(region);
    AWSCredentialsProvider credentialsProvider =
        new STSAssumeRoleSessionCredentialsProvider
            .Builder(awsCrossAccountAttributes.getCrossAccountRoleArn(), UUID.randomUUID().toString())
            .withExternalId(awsCrossAccountAttributes.getExternalId())
            .withStsClient(awsSecurityTokenService)
            .build();
    builder.withCredentials(credentialsProvider);
    return (AmazonEC2Client) builder.build();
  }
}
