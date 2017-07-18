package software.wings.service.impl;

import static com.google.api.client.repackaged.com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Singleton;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.codedeploy.AmazonCodeDeployClient;
import com.amazonaws.services.codedeploy.AmazonCodeDeployClientBuilder;
import com.amazonaws.services.codedeploy.model.AmazonCodeDeployException;
import com.amazonaws.services.codedeploy.model.CreateDeploymentRequest;
import com.amazonaws.services.codedeploy.model.CreateDeploymentResult;
import com.amazonaws.services.codedeploy.model.GetDeploymentGroupRequest;
import com.amazonaws.services.codedeploy.model.GetDeploymentGroupResult;
import com.amazonaws.services.codedeploy.model.GetDeploymentRequest;
import com.amazonaws.services.codedeploy.model.GetDeploymentResult;
import com.amazonaws.services.codedeploy.model.ListApplicationsRequest;
import com.amazonaws.services.codedeploy.model.ListApplicationsResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentConfigsRequest;
import com.amazonaws.services.codedeploy.model.ListDeploymentConfigsResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentGroupsRequest;
import com.amazonaws.services.codedeploy.model.ListDeploymentGroupsResult;
import com.amazonaws.services.codedeploy.model.ListDeploymentInstancesRequest;
import com.amazonaws.services.codedeploy.model.ListDeploymentInstancesResult;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeAccountAttributesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ecr.AmazonECRClient;
import com.amazonaws.services.ecr.AmazonECRClientBuilder;
import com.amazonaws.services.ecr.model.DescribeRepositoriesRequest;
import com.amazonaws.services.ecr.model.DescribeRepositoriesResult;
import com.amazonaws.services.ecr.model.GetAuthorizationTokenRequest;
import com.amazonaws.services.ecr.model.ListImagesRequest;
import com.amazonaws.services.ecr.model.ListImagesResult;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.AmazonECSException;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.EcrConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.SettingAttribute;
import software.wings.exception.WingsException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

// import com.amazonaws.services.ecr.A

/**
 * Created by anubhaw on 12/15/16.
 */
@Singleton
public class AwsHelperService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  public void validateAwsAccountCredential(String accessKey, char[] secretKey) {
    try {
      getAmazonEc2Client(Regions.US_EAST_1.getName(), accessKey, secretKey)
          .describeAccountAttributes(new DescribeAccountAttributesRequest());
    } catch (AmazonEC2Exception amazonEC2Exception) {
      if (amazonEC2Exception.getStatusCode() == 401) {
        throw new WingsException(ErrorCode.INVALID_CLOUD_PROVIDER, "message", "Invalid AWS credentials.");
      }
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
   * @param region    the region
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

  public AmazonECRClient getAmazonEcrClient(String region, String accessKey, char[] secretKey) {
    return (AmazonECRClient) AmazonECRClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
  }

  /**
   * Gets amazon ecr client.
   *
   * @param url       the url
   * @param region    the region
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the auth token
   */
  public static String getAmazonEcrAuthToken(String url, String region, String accessKey, char[] secretKey) {
    AmazonECRClient ecrClient = (AmazonECRClient) AmazonECRClientBuilder.standard()
                                    .withRegion(region)
                                    .withCredentials(new AWSStaticCredentialsProvider(
                                        new BasicAWSCredentials(accessKey, new String(secretKey))))
                                    .build();

    // https://830767422336.dkr.ecr.us-east-1.amazonaws.com/
    String awsAccount = url.substring(8, url.indexOf("."));

    return ecrClient
        .getAuthorizationToken(new GetAuthorizationTokenRequest().withRegistryIds(singletonList(awsAccount)))
        .getAuthorizationData()
        .get(0)
        .getAuthorizationToken();
  }

  /**
   * Gets amazon ec 2 client.
   *
   * @param region    the region
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

  /**
   * Gets amazon identity management client.
   *
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon identity management client
   */
  public AmazonIdentityManagementClient getAmazonIdentityManagementClient(String accessKey, char[] secretKey) {
    return new AmazonIdentityManagementClient(new BasicAWSCredentials(accessKey, new String(secretKey)));
  }

  /**
   * Gets amazon code deploy client.
   *
   * @param region    the region
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon code deploy client
   */
  private AmazonCodeDeployClient getAmazonCodeDeployClient(Regions region, String accessKey, char[] secretKey) {
    return (AmazonCodeDeployClient) AmazonCodeDeployClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
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

  /**
   * Gets amazon auto scaling client.
   *
   * @param region    the region
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon auto scaling client
   */
  public AmazonAutoScalingClient getAmazonAutoScalingClient(Regions region, String accessKey, char[] secretKey) {
    return (AmazonAutoScalingClient) AmazonAutoScalingClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
  }

  /**
   * Gets amazon elastic load balancing client.
   *
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon elastic load balancing client
   */
  public AmazonElasticLoadBalancingClient getAmazonElasticLoadBalancingClient(
      Regions region, String accessKey, char[] secretKey) {
    return (AmazonElasticLoadBalancingClient) com.amazonaws.services.elasticloadbalancingv2
        .AmazonElasticLoadBalancingClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
  }

  /**
   * Gets classic elb client.
   *
   * @param region    the region
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the classic elb client
   */
  public com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient getClassicElbClient(
      Regions region, String accessKey, char[] secretKey) {
    return (com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient)
        AmazonElasticLoadBalancingClientBuilder.standard()
            .withRegion(region)
            .withCredentials(
                new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
            .build();
  }

  /**
   * Gets hostname from dns name.
   *
   * @param dnsName the dns name
   * @return the hostname from dns name
   */
  public String getHostnameFromDnsName(String dnsName) {
    return (!isNullOrEmpty(dnsName) && dnsName.endsWith(".ec2.internal"))
        ? dnsName.substring(0, dnsName.length() - ".ec2.internal".length())
        : dnsName;
  }

  /**
   * Gets instance id.
   *
   * @param region    the region
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

  public AwsConfig validateAndGetAwsConfig(SettingAttribute connectorConfig) {
    if (connectorConfig == null || connectorConfig.getValue() == null
        || !(connectorConfig.getValue() instanceof AwsConfig)) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "connectorConfig is not of type AwsConfig");
    }
    return (AwsConfig) connectorConfig.getValue();
  }

  private void handleAmazonServiceException(AmazonServiceException amazonServiceException) {
    logger.error("AWS API call exception {}", amazonServiceException);
    if (amazonServiceException instanceof AmazonCodeDeployException) {
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED, new Throwable(amazonServiceException.getErrorMessage()));
    } else if (amazonServiceException instanceof AmazonEC2Exception) {
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED, "message", amazonServiceException.getErrorMessage());
    } else if (amazonServiceException instanceof AmazonECSException) {
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED, "message", amazonServiceException.getErrorMessage());
    }
    logger.error("Unhandled aws exception");
    throw new WingsException(ErrorCode.ACCESS_DENIED, "message", amazonServiceException.getErrorMessage());
  }

  public ListDeploymentGroupsResult listDeploymentGroupsResult(
      AwsConfig awsConfig, String region, ListDeploymentGroupsRequest listDeploymentGroupsRequest) {
    try {
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .listDeploymentGroups(listDeploymentGroupsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListDeploymentGroupsResult();
  }

  public ListApplicationsResult listApplicationsResult(
      AwsConfig awsConfig, String region, ListApplicationsRequest listApplicationsRequest) {
    try {
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .listApplications(listApplicationsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListApplicationsResult();
  }

  public ListDeploymentConfigsResult listDeploymentConfigsResult(
      AwsConfig awsConfig, String region, ListDeploymentConfigsRequest listDeploymentConfigsRequest) {
    try {
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .listDeploymentConfigs(listDeploymentConfigsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListDeploymentConfigsResult();
  }

  public GetDeploymentResult getCodeDeployDeployment(
      AwsConfig awsConfig, String region, GetDeploymentRequest getDeploymentRequest) {
    try {
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .getDeployment(getDeploymentRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new GetDeploymentResult();
  }

  public GetDeploymentGroupResult getCodeDeployDeploymentGroup(
      AwsConfig awsConfig, String region, GetDeploymentGroupRequest getDeploymentGroupRequest) {
    try {
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .getDeploymentGroup(getDeploymentGroupRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new GetDeploymentGroupResult();
  }

  public CreateDeploymentResult createCodeDeployDeployment(
      AwsConfig awsConfig, String region, CreateDeploymentRequest createDeploymentRequest) {
    try {
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .createDeployment(createDeploymentRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new CreateDeploymentResult();
  }

  public ListDeploymentInstancesResult listDeploymentInstances(
      AwsConfig awsConfig, String region, ListDeploymentInstancesRequest listDeploymentInstancesRequest) {
    try {
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .listDeploymentInstances(listDeploymentInstancesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListDeploymentInstancesResult();
  }

  public DescribeInstancesResult describeEc2Instances(
      AwsConfig awsConfig, String region, DescribeInstancesRequest describeInstancesRequest) {
    try {
      return getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeInstances(describeInstancesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeInstancesResult();
  }

  public ListImagesResult listEcrImages(EcrConfig ecrConfig, ListImagesRequest listImagesRequest) {
    try {
      return getAmazonEcrClient(ecrConfig.getRegion(), ecrConfig.getAccessKey(), ecrConfig.getSecretKey())
          .listImages(listImagesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListImagesResult();
  }

  public DescribeRepositoriesResult listRepositories(
      EcrConfig ecrConfig, DescribeRepositoriesRequest describeRepositoriesRequest) {
    try {
      return getAmazonEcrClient(ecrConfig.getRegion(), ecrConfig.getAccessKey(), ecrConfig.getSecretKey())
          .describeRepositories(describeRepositoriesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeRepositoriesResult();
  }

  public List<LoadBalancerDescription> getLoadBalancerDescriptions(String region, AwsConfig awsConfig) {
    try {
      return getClassicElbClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeLoadBalancers(
              new com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest().withPageSize(400))
          .getLoadBalancerDescriptions();
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ArrayList<>();
  }

  public List<TargetGroup> listTargetGroupsForElb(String region, String loadBalancerName, AwsConfig awsConfig) {
    try {
      AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient = getAmazonElasticLoadBalancingClient(
          Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());

      String loadBalancerArn =
          amazonElasticLoadBalancingClient
              .describeLoadBalancers(new DescribeLoadBalancersRequest().withNames(loadBalancerName))
              .getLoadBalancers()
              .get(0)
              .getLoadBalancerArn();

      return amazonElasticLoadBalancingClient
          .describeTargetGroups(
              new DescribeTargetGroupsRequest().withPageSize(400).withLoadBalancerArn(loadBalancerArn))
          .getTargetGroups();
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ArrayList<>();
  }
}
