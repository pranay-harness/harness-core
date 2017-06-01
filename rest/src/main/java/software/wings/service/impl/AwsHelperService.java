package software.wings.service.impl;

import static com.google.api.client.repackaged.com.google.common.base.Strings.isNullOrEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Singleton;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeAccountAttributesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.exception.WingsException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by anubhaw on 12/15/16.
 */
@Singleton
public class AwsHelperService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  public void validateCredential(String accessKey, char[] secretKey) {
    try {
      getAmazonEc2Client(Regions.US_EAST_1.getName(), accessKey, secretKey)
          .describeAccountAttributes(new DescribeAccountAttributesRequest());
    } catch (AmazonEC2Exception amazonEC2Exception) {
      throw new WingsException(ErrorCode.INVALID_CLOUD_PROVIDER, "message", "Invalid AWS credentials.");
    }
  }

  /**
   * Gets aws cloud watch client.
   *
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the aws cloud watch client
   */
  public AmazonCloudWatchClient getAwsCloudWatchClient(String accessKey, char[] secretKey) {
    return new AmazonCloudWatchClient(new BasicAWSCredentials(accessKey, new String(secretKey)));
  }

  /**
   * Gets amazon ecs client.
   *
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon ecs client
   */
  public AmazonECSClient getAmazonEcsClient(String region, String accessKey, char[] secretKey) {
    return (AmazonECSClient) AmazonECSClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
  }

  /**
   * Gets amazon ec 2 client.
   *
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon ec 2 client
   */
  public AmazonEC2Client getAmazonEc2Client(String region, String accessKey, char[] secretKey) {
    return (AmazonEC2Client) AmazonEC2ClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
  }

  public AmazonIdentityManagementClient getAmazonIdentityManagementClient(String accessKey, char[] secretKey) {
    return new AmazonIdentityManagementClient(new BasicAWSCredentials(accessKey, new String(secretKey)));
  }

  /**
   * Gets amazon cloud formation client.
   *
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon cloud formation client
   */
  public AmazonCloudFormationClient getAmazonCloudFormationClient(String accessKey, char[] secretKey) {
    return new AmazonCloudFormationClient(new BasicAWSCredentials(accessKey, new String(secretKey)));
  }

  public AmazonAutoScalingClient getAmazonAutoScalingClient(Regions region, String accessKey, char[] secretKey) {
    return (AmazonAutoScalingClient) AmazonAutoScalingClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
  }

  public AmazonElasticLoadBalancingClient getAmazonElasticLoadBalancingClient(String accessKey, char[] secretKey) {
    return new AmazonElasticLoadBalancingClient(new BasicAWSCredentials(accessKey, new String(secretKey)));
  }

  public com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient getClassicElbClient(
      Regions region, String accessKey, char[] secretKey) {
    return (com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient)
        AmazonElasticLoadBalancingClientBuilder.standard()
            .withRegion(region)
            .withCredentials(
                new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
            .build();
  }

  public String getHostnameFromDnsName(String dnsName) {
    return (!isNullOrEmpty(dnsName) && dnsName.endsWith(".ec2.internal"))
        ? dnsName.substring(0, dnsName.length() - ".ec2.internal".length())
        : dnsName;
  }

  /**
   * Gets instance id.
   *
   * @param region
   * @param accessKey the access key
   * @param secretKey the secret key
   * @param hostName  the host name
   * @return the instance id
   */
  public String getInstanceId(Regions region, String accessKey, char[] secretKey, String hostName) {
    AmazonEC2Client amazonEC2Client = getAmazonEc2Client(region.getName(), accessKey, secretKey);

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
