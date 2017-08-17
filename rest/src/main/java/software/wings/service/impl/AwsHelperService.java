package software.wings.service.impl;

import static com.google.api.client.repackaged.com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupResult;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.Metric;
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
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.ecr.AmazonECRClient;
import com.amazonaws.services.ecr.AmazonECRClientBuilder;
import com.amazonaws.services.ecr.model.DescribeRepositoriesRequest;
import com.amazonaws.services.ecr.model.DescribeRepositoriesResult;
import com.amazonaws.services.ecr.model.GetAuthorizationTokenRequest;
import com.amazonaws.services.ecr.model.ListImagesRequest;
import com.amazonaws.services.ecr.model.ListImagesResult;
import com.amazonaws.services.ecr.model.Repository;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.AmazonECSException;
import com.amazonaws.services.ecs.model.ClientException;
import com.amazonaws.services.ecs.model.CreateClusterRequest;
import com.amazonaws.services.ecs.model.CreateClusterResult;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.CreateServiceResult;
import com.amazonaws.services.ecs.model.DeleteServiceRequest;
import com.amazonaws.services.ecs.model.DeleteServiceResult;
import com.amazonaws.services.ecs.model.DescribeClustersRequest;
import com.amazonaws.services.ecs.model.DescribeClustersResult;
import com.amazonaws.services.ecs.model.DescribeContainerInstancesRequest;
import com.amazonaws.services.ecs.model.DescribeContainerInstancesResult;
import com.amazonaws.services.ecs.model.DescribeServicesRequest;
import com.amazonaws.services.ecs.model.DescribeServicesResult;
import com.amazonaws.services.ecs.model.DescribeTasksRequest;
import com.amazonaws.services.ecs.model.DescribeTasksResult;
import com.amazonaws.services.ecs.model.ListClustersRequest;
import com.amazonaws.services.ecs.model.ListClustersResult;
import com.amazonaws.services.ecs.model.ListServicesRequest;
import com.amazonaws.services.ecs.model.ListServicesResult;
import com.amazonaws.services.ecs.model.ListTasksRequest;
import com.amazonaws.services.ecs.model.ListTasksResult;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionResult;
import com.amazonaws.services.ecs.model.UpdateServiceRequest;
import com.amazonaws.services.ecs.model.UpdateServiceResult;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.Role;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

  public void validateAwsAccountCredential(EcrConfig ecrConfig) {
    validateAwsAccountCredential(ecrConfig.getAccessKey(), ecrConfig.getSecretKey());
  }

  /**
   * Gets aws cloud watch client.
   *
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the aws cloud watch client
   */
  private AmazonCloudWatchClient getAwsCloudWatchClient(String accessKey, char[] secretKey) {
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
  private AmazonECSClient getAmazonEcsClient(String region, String accessKey, char[] secretKey) {
    return (AmazonECSClient) AmazonECSClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
  }

  private AmazonECRClient getAmazonEcrClient(AwsConfig awsConfig, String region) {
    return (AmazonECRClient) AmazonECRClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(awsConfig.getAccessKey(), new String(awsConfig.getSecretKey()))))
        .build();
  }

  public AmazonECRClient getAmazonEcrClient(EcrConfig ecrConfig) {
    return (AmazonECRClient) AmazonECRClientBuilder.standard()
        .withRegion(ecrConfig.getRegion())
        .withCredentials(new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(ecrConfig.getAccessKey(), new String(ecrConfig.getSecretKey()))))
        .build();
  }

  /**
   * Gets amazon ecr client.
   *
   * @return the auth token
   */
  public String getAmazonEcrAuthToken(EcrConfig ecrConfig) {
    AmazonECRClient ecrClient = (AmazonECRClient) AmazonECRClientBuilder.standard()
                                    .withRegion(ecrConfig.getRegion())
                                    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(
                                        ecrConfig.getAccessKey(), new String(ecrConfig.getSecretKey()))))
                                    .build();

    String url = ecrConfig.getEcrUrl();
    // Example: https://830767422336.dkr.ecr.us-east-1.amazonaws.com/
    String awsAccount = url.substring(8, url.indexOf("."));
    return ecrClient
        .getAuthorizationToken(new GetAuthorizationTokenRequest().withRegistryIds(singletonList(awsAccount)))
        .getAuthorizationData()
        .get(0)
        .getAuthorizationToken();
  }

  public String getAmazonEcrAuthToken(String awsAccount, String region, String accessKey, char[] secretKey) {
    AmazonECRClient ecrClient = (AmazonECRClient) AmazonECRClientBuilder.standard()
                                    .withRegion(region)
                                    .withCredentials(new AWSStaticCredentialsProvider(
                                        new BasicAWSCredentials(accessKey, new String(secretKey))))
                                    .build();
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
  private AmazonEC2Client getAmazonEc2Client(String region, String accessKey, char[] secretKey) {
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
  private AmazonIdentityManagementClient getAmazonIdentityManagementClient(String accessKey, char[] secretKey) {
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
  private AmazonCloudFormationClient getAmazonCloudFormationClient(String accessKey, char[] secretKey) {
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
  private AmazonAutoScalingClient getAmazonAutoScalingClient(Regions region, String accessKey, char[] secretKey) {
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
  private AmazonElasticLoadBalancingClient getAmazonElasticLoadBalancingClient(
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
  private com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient getClassicElbClient(
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
      logger.error(e.getMessage(), e);
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
    logger.error("AWS API call exception", amazonServiceException);
    if (amazonServiceException instanceof AmazonCodeDeployException) {
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED, new Throwable(amazonServiceException.getErrorMessage()));
    } else if (amazonServiceException instanceof AmazonEC2Exception) {
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED, "message", amazonServiceException.getErrorMessage());
    } else if (amazonServiceException instanceof AmazonECSException) {
      if (amazonServiceException instanceof ClientException) {
        logger.warn(amazonServiceException.getErrorMessage(), amazonServiceException);
        throw amazonServiceException;
      }
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED, "message", amazonServiceException.getErrorMessage());
    } else {
      logger.error("Unhandled aws exception");
      throw new WingsException(ErrorCode.ACCESS_DENIED, "message", amazonServiceException.getErrorMessage());
    }
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
  public TerminateInstancesResult terminateEc2Instances(AwsConfig awsConfig, String region, List<String> instancesIds) {
    try {
      AmazonEC2Client amazonEc2Client = getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonEc2Client.terminateInstances(new TerminateInstancesRequest(instancesIds));
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new TerminateInstancesResult();
  }

  public DescribeImagesResult decribeEc2Images(
      AwsConfig awsConfig, String region, DescribeImagesRequest describeImagesRequest) {
    try {
      AmazonEC2Client amazonEc2Client = getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonEc2Client.describeImages(describeImagesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeImagesResult();
  }

  public List<String> listRegions(AwsConfig awsConfig) {
    try {
      AmazonEC2Client amazonEC2Client =
          getAmazonEc2Client(Regions.US_EAST_1.getName(), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonEC2Client.describeRegions().getRegions().stream().map(Region::getRegionName).collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return Arrays.asList();
  }

  public List<String> listVPCs(AwsConfig awsConfig, String region) {
    List<String> results = Lists.newArrayList();
    try {
      AmazonEC2Client amazonEC2Client = getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
      results.addAll(amazonEC2Client
                         .describeVpcs(new DescribeVpcsRequest().withFilters(
                             new Filter().withName("state").withValues("available"),
                             new Filter().withName("isDefault").withValues("true")))
                         .getVpcs()
                         .stream()
                         .map(Vpc::getVpcId)
                         .collect(toList()));
      results.addAll(amazonEC2Client
                         .describeVpcs(new DescribeVpcsRequest().withFilters(
                             new Filter().withName("state").withValues("available"),
                             new Filter().withName("isDefault").withValues("false")))
                         .getVpcs()
                         .stream()
                         .map(Vpc::getVpcId)
                         .collect(toList()));
      return results;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return results;
  }

  public CreateClusterResult createCluster(
      String region, AwsConfig awsConfig, CreateClusterRequest createClusterRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .createCluster(createClusterRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new CreateClusterResult();
  }
  public DescribeClustersResult describeClusters(
      String region, AwsConfig awsConfig, DescribeClustersRequest describeClustersRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeClusters(describeClustersRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeClustersResult();
  }
  public ListClustersResult listClusters(String region, AwsConfig awsConfig, ListClustersRequest listClustersRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .listClusters(listClustersRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListClustersResult();
  }

  public RegisterTaskDefinitionResult registerTaskDefinition(
      String region, AwsConfig awsConfig, RegisterTaskDefinitionRequest registerTaskDefinitionRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .registerTaskDefinition(registerTaskDefinitionRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new RegisterTaskDefinitionResult();
  }
  public ListServicesResult listServices(String region, AwsConfig awsConfig, ListServicesRequest listServicesRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .listServices(listServicesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListServicesResult();
  }
  public DescribeServicesResult describeServices(
      String region, AwsConfig awsConfig, DescribeServicesRequest describeServicesRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeServices(describeServicesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeServicesResult();
  }
  public CreateServiceResult createService(
      String region, AwsConfig awsConfig, CreateServiceRequest createServiceRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .createService(createServiceRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new CreateServiceResult();
  }
  public UpdateServiceResult updateService(
      String region, AwsConfig awsConfig, UpdateServiceRequest updateServiceRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .updateService(updateServiceRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new UpdateServiceResult();
  }

  public DeleteServiceResult deleteService(
      String region, AwsConfig awsConfig, DeleteServiceRequest deleteServiceRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .deleteService(deleteServiceRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DeleteServiceResult();
  }

  public ListTasksResult listTasks(String region, AwsConfig awsConfig, ListTasksRequest listTasksRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey()).listTasks(listTasksRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListTasksResult();
  }
  public DescribeTasksResult describeTasks(
      String region, AwsConfig awsConfig, DescribeTasksRequest describeTasksRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeTasks(describeTasksRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeTasksResult();
  }
  public DescribeContainerInstancesResult describeContainerInstances(
      String region, AwsConfig awsConfig, DescribeContainerInstancesRequest describeContainerInstancesRequest) {
    try {
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeContainerInstances(describeContainerInstancesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeContainerInstancesResult();
  }

  public ListImagesResult listEcrImages(AwsConfig awsConfig, String region, ListImagesRequest listImagesRequest) {
    try {
      return getAmazonEcrClient(awsConfig, region).listImages(listImagesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListImagesResult();
  }

  public ListImagesResult listEcrImages(EcrConfig ecrConfig, ListImagesRequest listImagesRequest) {
    try {
      return getAmazonEcrClient(ecrConfig).listImages(listImagesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListImagesResult();
  }

  public DescribeRepositoriesResult listRepositories(
      EcrConfig ecrConfig, DescribeRepositoriesRequest describeRepositoriesRequest) {
    try {
      return getAmazonEcrClient(ecrConfig).describeRepositories(describeRepositoriesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeRepositoriesResult();
  }

  public DescribeRepositoriesResult listRepositories(
      AwsConfig awsConfig, DescribeRepositoriesRequest describeRepositoriesRequest, String region) {
    try {
      return getAmazonEcrClient(awsConfig, region).describeRepositories(describeRepositoriesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeRepositoriesResult();
  }

  public Repository getRepository(AwsConfig awsConfig, String region, String repositoryName) {
    DescribeRepositoriesRequest describeRepositoriesRequest = new DescribeRepositoriesRequest();
    describeRepositoriesRequest.setRepositoryNames(Lists.newArrayList(repositoryName));
    DescribeRepositoriesResult describeRepositoriesResult =
        listRepositories(awsConfig, describeRepositoriesRequest, region);
    List<Repository> repositories = describeRepositoriesResult.getRepositories();
    if (repositories != null && repositories.size() > 0) {
      return repositories.get(0);
    }
    return null;
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

  public List<TargetGroup> listTargetGroupsForElb(String region, AwsConfig awsConfig, String loadBalancerName) {
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

  public Map<String, String> listRoles(AwsConfig awsConfig) {
    try {
      AmazonIdentityManagementClient amazonIdentityManagementClient =
          getAmazonIdentityManagementClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());

      return amazonIdentityManagementClient.listRoles(new ListRolesRequest().withMaxItems(400))
          .getRoles()
          .stream()
          .collect(Collectors.toMap(Role::getArn, Role::getRoleName));
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return Maps.newHashMap();
  }

  public List<Instance> listRunInstances(
      AwsConfig awsConfig, String region, String launcherConfigName, int instanceCount) {
    try {
      AmazonAutoScalingClient amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      AmazonEC2Client amazonEc2Client = getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());

      List<LaunchConfiguration> launchConfigurations =
          amazonAutoScalingClient
              .describeLaunchConfigurations(
                  new DescribeLaunchConfigurationsRequest().withLaunchConfigurationNames(launcherConfigName))
              .getLaunchConfigurations();
      LaunchConfiguration launchConfiguration = launchConfigurations.get(0);

      RunInstancesRequest runInstancesRequest =
          new RunInstancesRequest()
              .withImageId(launchConfiguration.getImageId())
              .withInstanceType(launchConfiguration.getInstanceType())
              .withMinCount(instanceCount)
              .withMaxCount(instanceCount)
              .withKeyName(launchConfiguration.getKeyName())
              .withIamInstanceProfile(
                  new IamInstanceProfileSpecification().withName(launchConfiguration.getIamInstanceProfile()))
              .withSecurityGroupIds(launchConfiguration.getSecurityGroups())
              .withUserData(launchConfiguration.getUserData());

      List<Instance> instances = amazonEc2Client.runInstances(runInstancesRequest).getReservation().getInstances();
      return instances;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return Arrays.asList();
  }

  public List<String> listIAMInstanceRoles(AwsConfig awsConfig) {
    try {
      AmazonIdentityManagementClient amazonIdentityManagementClient =
          getAmazonIdentityManagementClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonIdentityManagementClient.listInstanceProfiles()
          .getInstanceProfiles()
          .stream()
          .map(InstanceProfile::getInstanceProfileName)
          .collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return Arrays.asList();
  }

  public Map<String, String> listIAMRoles(AwsConfig awsConfig) {
    try {
      AmazonIdentityManagementClient amazonIdentityManagementClient =
          getAmazonIdentityManagementClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());

      return amazonIdentityManagementClient.listRoles(new ListRolesRequest().withMaxItems(400))
          .getRoles()
          .stream()
          .collect(Collectors.toMap(Role::getArn, Role::getRoleName));
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return Maps.newHashMap();
  }

  public List<String> listApplicationLoadBalancers(AwsConfig awsConfig, String region) {
    try {
      AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient = getAmazonElasticLoadBalancingClient(
          Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonElasticLoadBalancingClient
          .describeLoadBalancers(new DescribeLoadBalancersRequest().withPageSize(400))
          .getLoadBalancers()
          .stream()
          .filter(loadBalancer -> StringUtils.equalsIgnoreCase(loadBalancer.getType(), "application"))
          .map(LoadBalancer::getLoadBalancerName)
          .collect(Collectors.toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return Arrays.asList();
  }

  public List<String> listClassicLoadBalancers(AwsConfig awsConfig, String region) {
    try {
      List<LoadBalancerDescription> describeLoadBalancers = getLoadBalancerDescriptions(region, awsConfig);
      return describeLoadBalancers.stream()
          .map(LoadBalancerDescription::getLoadBalancerName)
          .collect(Collectors.toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ArrayList<>();
  }

  public List<LaunchConfiguration> describeLaunchConfigurations(AwsConfig awsConfig, String region) {
    try {
      AmazonAutoScalingClient amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      // TODO:: remove direct usage of LaunchConfiguration
      return amazonAutoScalingClient.describeLaunchConfigurations().getLaunchConfigurations();
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return Arrays.asList();
  }

  public CreateAutoScalingGroupResult createAutoScalingGroup(
      AwsConfig awsConfig, String region, CreateAutoScalingGroupRequest createAutoScalingGroupRequest) {
    try {
      AmazonAutoScalingClient amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      CreateAutoScalingGroupResult createAutoScalingGroupResult =
          amazonAutoScalingClient.createAutoScalingGroup(createAutoScalingGroupRequest);
      return createAutoScalingGroupResult;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new CreateAutoScalingGroupResult();
  }

  public DescribeAutoScalingGroupsResult describeAutoScalingGroups(
      AwsConfig awsConfig, String region, DescribeAutoScalingGroupsRequest autoScalingGroupsRequest) {
    try {
      AmazonAutoScalingClient amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonAutoScalingClient.describeAutoScalingGroups(autoScalingGroupsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeAutoScalingGroupsResult();
  }

  public Datapoint getCloudWatchMetricStatistics(
      AwsConfig awsConfig, GetMetricStatisticsRequest metricStatisticsRequest) {
    try {
      AmazonCloudWatchClient cloudWatchClient =
          getAwsCloudWatchClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());
      Datapoint datapoint = cloudWatchClient.getMetricStatistics(metricStatisticsRequest)
                                .getDatapoints()
                                .stream()
                                .max(Comparator.comparing(Datapoint::getTimestamp))
                                .orElse(null);
      return datapoint;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new Datapoint();
  }

  public List<Metric> getCloudWatchMetrics(AwsConfig awsConfig) {
    try {
      AmazonCloudWatchClient cloudWatchClient =
          getAwsCloudWatchClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return cloudWatchClient.listMetrics().getMetrics();

    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return Arrays.asList();
  }
  public List<Metric> getCloudWatchMetrics(AwsConfig awsConfig, ListMetricsRequest listMetricsRequest) {
    try {
      AmazonCloudWatchClient cloudWatchClient =
          getAwsCloudWatchClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return cloudWatchClient.listMetrics(listMetricsRequest).getMetrics();

    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return Arrays.asList();
  }

  public boolean registerInstancesWithLoadBalancer(
      Regions region, String accessKey, char[] secretKey, String loadBalancerName, String instanceId) {
    try {
      com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient elbClient =
          getClassicElbClient(region, accessKey, secretKey);
      return elbClient
          .registerInstancesWithLoadBalancer(
              new RegisterInstancesWithLoadBalancerRequest()
                  .withLoadBalancerName(loadBalancerName)
                  .withInstances(new com.amazonaws.services.elasticloadbalancing.model.Instance(instanceId)))
          .getInstances()
          .stream()
          .anyMatch(inst -> inst.getInstanceId().equals(instanceId));
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return false;
  }

  public boolean deregisterInstancesFromLoadBalancer(
      Regions region, String accessKey, char[] secretKey, String loadBalancerName, String instanceId) {
    try {
      com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient elbClient =
          getClassicElbClient(region, accessKey, secretKey);
      return elbClient
          .deregisterInstancesFromLoadBalancer(
              new DeregisterInstancesFromLoadBalancerRequest()
                  .withLoadBalancerName(loadBalancerName)
                  .withInstances(new com.amazonaws.services.elasticloadbalancing.model.Instance(instanceId)))
          .getInstances()
          .stream()
          .noneMatch(inst -> inst.getInstanceId().equals(instanceId));
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return false;
  }

  public CreateStackResult createStack(String accessKey, char[] secretKey, CreateStackRequest createStackRequest) {
    try {
      AmazonCloudFormationClient cloudFormationClient = getAmazonCloudFormationClient(accessKey, secretKey);
      return cloudFormationClient.createStack(createStackRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new CreateStackResult();
  }

  public DescribeStacksResult describeStacks(
      String accessKey, char[] secretKey, DescribeStacksRequest describeStacksRequest) {
    try {
      AmazonCloudFormationClient cloudFormationClient = getAmazonCloudFormationClient(accessKey, secretKey);
      return cloudFormationClient.describeStacks(describeStacksRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeStacksResult();
  }
}
