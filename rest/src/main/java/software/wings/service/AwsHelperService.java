package software.wings.service;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Singleton;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;

/**
 * Created by anubhaw on 12/15/16.
 */

@Singleton
public class AwsHelperService {
  public AmazonCloudWatchClient getAwsCloudWatchClient(String accessKey, String secretKey) {
    return new AmazonCloudWatchClient(new BasicAWSCredentials(accessKey, secretKey));
  }

  public String getInstanceId(String accessKey, String secretKey, String hostName) {
    AmazonEC2Client amazonEC2Client = new AmazonEC2Client(new BasicAWSCredentials(accessKey, secretKey));

    String instanceId;
    DescribeInstancesResult describeInstancesResult =
        amazonEC2Client.describeInstances(new DescribeInstancesRequest().withFilters(
            new Filter().withName("private-dns-name").withValues(hostName + "*")));
    instanceId = describeInstancesResult.getReservations()
                     .stream()
                     .flatMap(reservation -> reservation.getInstances().stream())
                     .map(Instance::getInstanceId)
                     .findFirst()
                     .orElse(null);

    if (isBlank(instanceId)) {
      describeInstancesResult = amazonEC2Client.describeInstances(
          new DescribeInstancesRequest().withFilters(new Filter().withName("private-ip-address").withValues(hostName)));
      instanceId = describeInstancesResult.getReservations()
                       .stream()
                       .flatMap(reservation -> reservation.getInstances().stream())
                       .map(Instance::getInstanceId)
                       .findFirst()
                       .orElse(instanceId);
    }

    if (isBlank(instanceId)) {
      describeInstancesResult = amazonEC2Client.describeInstances(
          new DescribeInstancesRequest().withFilters(new Filter().withName("dns-name").withValues(hostName + "*")));
      instanceId = describeInstancesResult.getReservations()
                       .stream()
                       .flatMap(reservation -> reservation.getInstances().stream())
                       .map(Instance::getInstanceId)
                       .findFirst()
                       .orElse(instanceId);
    }

    if (isBlank(instanceId)) {
      describeInstancesResult = amazonEC2Client.describeInstances(
          new DescribeInstancesRequest().withFilters(new Filter().withName("ip-address").withValues(hostName)));
      instanceId = describeInstancesResult.getReservations()
                       .stream()
                       .flatMap(reservation -> reservation.getInstances().stream())
                       .map(Instance::getInstanceId)
                       .findFirst()
                       .orElse(instanceId);
    }
    return instanceId;
  }
}
