package software.wings.service.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Singleton;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by anubhaw on 12/15/16.
 */
@Singleton
public class AwsHelperService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Gets aws cloud watch client.
   *
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the aws cloud watch client
   */
  public AmazonCloudWatchClient getAwsCloudWatchClient(String accessKey, String secretKey) {
    return new AmazonCloudWatchClient(new BasicAWSCredentials(accessKey, secretKey));
  }

  /**
   * Gets amazon ecs client.
   *
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon ecs client
   */
  public AmazonECSClient getAmazonEcsClient(String accessKey, String secretKey) {
    return new AmazonECSClient(new BasicAWSCredentials(accessKey, secretKey));
  }

  /**
   * Gets amazon ec 2 client.
   *
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon ec 2 client
   */
  public AmazonEC2Client getAmazonEc2Client(String accessKey, String secretKey) {
    return new AmazonEC2Client(new BasicAWSCredentials(accessKey, secretKey));
  }

  public AmazonIdentityManagementClient getAmazonIdentityManagementClient(String accessKey, String secretKey) {
    return new AmazonIdentityManagementClient(new BasicAWSCredentials(accessKey, secretKey));
  }

  /**
   * Gets amazon cloud formation client.
   *
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon cloud formation client
   */
  public AmazonCloudFormationClient getAmazonCloudFormationClient(String accessKey, String secretKey) {
    return new AmazonCloudFormationClient(new BasicAWSCredentials(accessKey, secretKey));
  }

  /**
   * Gets amazon auto scaling client.
   *
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon auto scaling client
   */
  public AmazonAutoScalingClient getAmazonAutoScalingClient(String accessKey, String secretKey) {
    return new AmazonAutoScalingClient(new BasicAWSCredentials(accessKey, secretKey));
  }

  /**
   * Gets instance id.
   *
   * @param accessKey the access key
   * @param secretKey the secret key
   * @param hostName  the host name
   * @return the instance id
   */
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

  /**
   * Gets id from arn.
   *
   * @param arn the arn
   * @return the id from arn
   */
  public String getIdFromArn(String arn) {
    return arn.substring(arn.lastIndexOf('/') + 1);
  }

  /**
   * Can connect to host boolean.
   *
   * @param hostName the host name
   * @param port     the port
   * @param timeout  the timeout
   * @return the boolean
   */
  public boolean canConnectToHost(String hostName, int port, int timeout) {
    Socket client = new Socket();
    try {
      client.connect(new InetSocketAddress(hostName, port), timeout);
      client.close();
      return true;
    } catch (IOException e) {
      logger.error(e.getMessage());
      e.printStackTrace();
      return false;
    } finally {
      IOUtils.closeQuietly(client);
    }
  }
}
