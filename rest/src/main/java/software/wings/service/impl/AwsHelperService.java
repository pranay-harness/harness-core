package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static software.wings.beans.ErrorCode.INIT_TIMEOUT;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.Activity;
import com.amazonaws.services.autoscaling.model.AmazonAutoScalingException;
import com.amazonaws.services.autoscaling.model.AttachLoadBalancerTargetGroupsRequest;
import com.amazonaws.services.autoscaling.model.AttachLoadBalancerTargetGroupsResult;
import com.amazonaws.services.autoscaling.model.AttachLoadBalancersRequest;
import com.amazonaws.services.autoscaling.model.AttachLoadBalancersResult;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupResult;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationResult;
import com.amazonaws.services.autoscaling.model.DeleteAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsResult;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesRequest;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesResult;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.autoscaling.model.SetDesiredCapacityRequest;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.amazonaws.services.cloudformation.model.UpdateStackResult;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.ListMetricsResult;
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
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.ResourceType;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.TagDescription;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.amazonaws.services.ec2.model.Vpc;
import com.amazonaws.services.ecr.AmazonECRClient;
import com.amazonaws.services.ecr.AmazonECRClientBuilder;
import com.amazonaws.services.ecr.model.AmazonECRException;
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
import com.amazonaws.services.ecs.model.ClusterNotFoundException;
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
import com.amazonaws.services.ecs.model.DescribeTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.DescribeTaskDefinitionResult;
import com.amazonaws.services.ecs.model.DescribeTasksRequest;
import com.amazonaws.services.ecs.model.DescribeTasksResult;
import com.amazonaws.services.ecs.model.ListClustersRequest;
import com.amazonaws.services.ecs.model.ListClustersResult;
import com.amazonaws.services.ecs.model.ListServicesRequest;
import com.amazonaws.services.ecs.model.ListServicesResult;
import com.amazonaws.services.ecs.model.ListTaskDefinitionsRequest;
import com.amazonaws.services.ecs.model.ListTaskDefinitionsResult;
import com.amazonaws.services.ecs.model.ListTasksRequest;
import com.amazonaws.services.ecs.model.ListTasksResult;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionResult;
import com.amazonaws.services.ecs.model.ServiceNotFoundException;
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
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.CreateAliasRequest;
import com.amazonaws.services.lambda.model.CreateAliasResult;
import com.amazonaws.services.lambda.model.CreateFunctionRequest;
import com.amazonaws.services.lambda.model.CreateFunctionResult;
import com.amazonaws.services.lambda.model.GetFunctionRequest;
import com.amazonaws.services.lambda.model.GetFunctionResult;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.ListAliasesRequest;
import com.amazonaws.services.lambda.model.ListAliasesResult;
import com.amazonaws.services.lambda.model.ListFunctionsRequest;
import com.amazonaws.services.lambda.model.ListFunctionsResult;
import com.amazonaws.services.lambda.model.ListVersionsByFunctionRequest;
import com.amazonaws.services.lambda.model.ListVersionsByFunctionResult;
import com.amazonaws.services.lambda.model.PublishVersionRequest;
import com.amazonaws.services.lambda.model.PublishVersionResult;
import com.amazonaws.services.lambda.model.ResourceNotFoundException;
import com.amazonaws.services.lambda.model.UpdateAliasRequest;
import com.amazonaws.services.lambda.model.UpdateAliasResult;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeResult;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationResult;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.api.DeploymentType;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInstanceFilter;
import software.wings.beans.EcrConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.LogCallback;
import software.wings.common.Constants;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.expression.ExpressionEvaluator;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.states.ManagerExecutionLogCallback;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by anubhaw on 12/15/16.
 */
@Singleton
public class AwsHelperService {
  private static final String AWS_AVAILABILITY_ZONE_CHECK =
      "http://169.254.169.254/latest/meta-data/placement/availability-zone";
  @Inject private EncryptionService encryptionService;
  @Inject private TimeLimiter timeLimiter;
  @Inject private ExpressionEvaluator expressionEvaluator;

  private static final long AUTOSCALING_REQUEST_STATUS_CHECK_INTERVAL = TimeUnit.SECONDS.toSeconds(15);

  private static final Logger logger = LoggerFactory.getLogger(AwsHelperService.class);

  public boolean validateAwsAccountCredential(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      getAmazonEc2Client(Regions.US_EAST_1.getName(), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeRegions();
    } catch (AmazonEC2Exception amazonEC2Exception) {
      if (amazonEC2Exception.getStatusCode() == 401) {
        throw new WingsException(ErrorCode.INVALID_CLOUD_PROVIDER).addParam("message", "Invalid AWS credentials.");
      }
    }
    return true;
  }

  public void validateAwsAccountCredential(String accessKey, char[] secretKey) {
    try {
      getAmazonEc2Client(Regions.US_EAST_1.getName(), accessKey, secretKey).describeRegions();
    } catch (AmazonEC2Exception amazonEC2Exception) {
      if (amazonEC2Exception.getStatusCode() == 401) {
        throw new WingsException(ErrorCode.INVALID_CLOUD_PROVIDER).addParam("message", "Invalid AWS credentials.");
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
  private AmazonCloudWatchClient getAwsCloudWatchClient(String region, String accessKey, char[] secretKey) {
    return (AmazonCloudWatchClient) AmazonCloudWatchClientBuilder.standard()
        .withRegion(region)
        .withCredentials(
            new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, String.valueOf(secretKey))))
        .build();
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

  private AWSLambdaClient getAmazonLambdaClient(String region, String accessKey, char[] secretKey) {
    return (AWSLambdaClient) AWSLambdaClientBuilder.standard()
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

  public AmazonECRClient getAmazonEcrClient(EcrConfig ecrConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    encryptionService.decrypt(ecrConfig, encryptedDataDetails);
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
  public String getAmazonEcrAuthToken(EcrConfig ecrConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    encryptionService.decrypt(ecrConfig, encryptedDataDetails);
    AmazonECRClient ecrClient = (AmazonECRClient) AmazonECRClientBuilder.standard()
                                    .withRegion(ecrConfig.getRegion())
                                    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(
                                        ecrConfig.getAccessKey(), new String(ecrConfig.getSecretKey()))))
                                    .build();

    String url = ecrConfig.getEcrUrl();
    // Example: https://830767422336.dkr.ecr.us-east-1.amazonaws.com/
    String awsAccount = url.substring(8, url.indexOf('.'));
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
   * Gets amazon s3 client.
   *
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon s3 client
   */
  private AmazonS3Client getAmazonS3Client(String accessKey, char[] secretKey, String region) {
    return (AmazonS3Client) AmazonS3ClientBuilder.standard()
        .withRegion(region)
        .withCredentials(
            new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, String.valueOf(secretKey))))
        .withForceGlobalBucketAccessEnabled(true)
        .build();
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
  @VisibleForTesting
  AmazonCloudFormationClient getAmazonCloudFormationClient(Regions region, String accessKey, char[] secretKey) {
    return (AmazonCloudFormationClient) AmazonCloudFormationClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
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
  public String getHostnameFromPrivateDnsName(String dnsName) {
    return isNotEmpty(dnsName) ? dnsName.split("\\.")[0] : "";
  }

  public String getHostnameFromConvention(Map<String, Object> context, String hostNameConvention) {
    if (isEmpty(hostNameConvention)) {
      hostNameConvention = Constants.DEFAULT_AWS_HOST_NAME_CONVENTION;
    }
    return expressionEvaluator.substitute(hostNameConvention, context);
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
    DescribeInstancesResult describeInstancesResult = amazonEC2Client.describeInstances(
        new DescribeInstancesRequest().withFilters(new Filter("private-dns-name").withValues(hostName + "*")));
    instanceId = describeInstancesResult.getReservations()
                     .stream()
                     .flatMap(reservation -> reservation.getInstances().stream())
                     .map(Instance::getInstanceId)
                     .findFirst()
                     .orElse(null);

    if (isBlank(instanceId)) {
      describeInstancesResult = amazonEC2Client.describeInstances(
          new DescribeInstancesRequest().withFilters(new Filter("private-ip-address").withValues(hostName)));
      instanceId = describeInstancesResult.getReservations()
                       .stream()
                       .flatMap(reservation -> reservation.getInstances().stream())
                       .map(Instance::getInstanceId)
                       .findFirst()
                       .orElse(instanceId);
    }

    if (isBlank(instanceId)) {
      describeInstancesResult = amazonEC2Client.describeInstances(
          new DescribeInstancesRequest().withFilters(new Filter("dns-name").withValues(hostName + "*")));
      instanceId = describeInstancesResult.getReservations()
                       .stream()
                       .flatMap(reservation -> reservation.getInstances().stream())
                       .map(Instance::getInstanceId)
                       .findFirst()
                       .orElse(instanceId);
    }

    if (isBlank(instanceId)) {
      describeInstancesResult = amazonEC2Client.describeInstances(
          new DescribeInstancesRequest().withFilters(new Filter("ip-address").withValues(hostName)));
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
    try (Socket client = new Socket()) {
      client.connect(new InetSocketAddress(hostName, port), timeout);
    } catch (IOException e) {
      return false;
    }
    return true;
  }

  public List<Bucket> listS3Buckets(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonS3Client(awsConfig.getAccessKey(), awsConfig.getSecretKey(), "us-east-1").listBuckets();
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return Collections.emptyList();
  }

  public S3Object getObjectFromS3(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName, String key) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonS3Client(
          awsConfig.getAccessKey(), awsConfig.getSecretKey(), getBucketRegion(awsConfig, encryptionDetails, bucketName))
          .getObject(bucketName, key);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return null;
  }

  public ObjectMetadata getObjectMetadataFromS3(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName, String key) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonS3Client(
          awsConfig.getAccessKey(), awsConfig.getSecretKey(), getBucketRegion(awsConfig, encryptionDetails, bucketName))
          .getObjectMetadata(bucketName, key);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return null;
  }

  public ListObjectsV2Result listObjectsInS3(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, ListObjectsV2Request listObjectsV2Request) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonS3Client amazonS3Client = getAmazonS3Client(awsConfig.getAccessKey(), awsConfig.getSecretKey(),
          getBucketRegion(awsConfig, encryptionDetails, listObjectsV2Request.getBucketName()));
      return amazonS3Client.listObjectsV2(listObjectsV2Request);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListObjectsV2Result();
  }

  public AwsConfig validateAndGetAwsConfig(
      SettingAttribute connectorConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    if (connectorConfig == null || connectorConfig.getValue() == null
        || !(connectorConfig.getValue() instanceof AwsConfig)) {
      throw new InvalidRequestException("connectorConfig is not of type AwsConfig");
    }
    encryptionService.decrypt((Encryptable) connectorConfig.getValue(), encryptedDataDetails);
    return (AwsConfig) connectorConfig.getValue();
  }

  private void handleAmazonServiceException(AmazonServiceException amazonServiceException) {
    logger.error("AWS API call exception", amazonServiceException);
    if (amazonServiceException instanceof AmazonCodeDeployException) {
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED).addParam("message", amazonServiceException.getMessage());
    } else if (amazonServiceException instanceof AmazonEC2Exception) {
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED).addParam("message", amazonServiceException.getMessage());
    } else if (amazonServiceException instanceof ClusterNotFoundException) {
      throw new WingsException(ErrorCode.AWS_CLUSTER_NOT_FOUND)
          .addParam("message", amazonServiceException.getMessage());
    } else if (amazonServiceException instanceof ServiceNotFoundException) {
      throw new WingsException(ErrorCode.AWS_SERVICE_NOT_FOUND)
          .addParam("message", amazonServiceException.getMessage());
    } else if (amazonServiceException instanceof AmazonAutoScalingException) {
      logger.warn(amazonServiceException.getErrorMessage(), amazonServiceException);
      throw amazonServiceException;
    } else if (amazonServiceException instanceof AmazonECSException
        || amazonServiceException instanceof AmazonECRException) {
      if (amazonServiceException instanceof ClientException) {
        logger.warn(amazonServiceException.getErrorMessage(), amazonServiceException);
        throw amazonServiceException;
      }
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED).addParam("message", amazonServiceException.getMessage());
    } else if (amazonServiceException instanceof AmazonCloudFormationException) {
      if (amazonServiceException.getMessage().contains("No updates are to be performed")) {
        logger.info("Nothing to update on stack" + amazonServiceException.getMessage());
      } else {
        throw new InvalidRequestException(amazonServiceException.getMessage(), amazonServiceException);
      }
    } else {
      logger.error("Unhandled aws exception");
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED).addParam("message", amazonServiceException.getMessage());
    }
  }

  public ListDeploymentGroupsResult listDeploymentGroupsResult(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region,
      ListDeploymentGroupsRequest listDeploymentGroupsRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .listDeploymentGroups(listDeploymentGroupsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListDeploymentGroupsResult();
  }

  public ListApplicationsResult listApplicationsResult(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, ListApplicationsRequest listApplicationsRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .listApplications(listApplicationsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListApplicationsResult();
  }

  public ListDeploymentConfigsResult listDeploymentConfigsResult(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region,
      ListDeploymentConfigsRequest listDeploymentConfigsRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .listDeploymentConfigs(listDeploymentConfigsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListDeploymentConfigsResult();
  }

  public GetDeploymentResult getCodeDeployDeployment(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, GetDeploymentRequest getDeploymentRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .getDeployment(getDeploymentRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new GetDeploymentResult();
  }

  public GetDeploymentGroupResult getCodeDeployDeploymentGroup(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region, GetDeploymentGroupRequest getDeploymentGroupRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .getDeploymentGroup(getDeploymentGroupRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new GetDeploymentGroupResult();
  }

  public CreateDeploymentResult createCodeDeployDeployment(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region, CreateDeploymentRequest createDeploymentRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .createDeployment(createDeploymentRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new CreateDeploymentResult();
  }

  public ListDeploymentInstancesResult listDeploymentInstances(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region,
      ListDeploymentInstancesRequest listDeploymentInstancesRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .listDeploymentInstances(listDeploymentInstancesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListDeploymentInstancesResult();
  }

  /**
   * Be a bit careful when calling this method.
   * When describeInstancesRequest.instanceIds is empty, AmazonEc2Client.describeInstances(), returns all instances
   * in aws farm or if filter is present then all the instances that match the filter  in describeInstancesRequest.
   * @param awsConfig
   * @param encryptionDetails
   * @param region
   * @param describeInstancesRequest
   * @return
   */
  public DescribeInstancesResult describeEc2Instances(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, DescribeInstancesRequest describeInstancesRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeInstances(describeInstancesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeInstancesResult();
  }

  public DescribeInstanceStatusResult describeEc2InstanceStatus(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region,
      DescribeInstanceStatusRequest describeInstanceStatusRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeInstanceStatus(describeInstanceStatusRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeInstanceStatusResult();
  }

  public TerminateInstancesResult terminateEc2Instances(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> instancesIds) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonEC2Client amazonEc2Client = getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonEc2Client.terminateInstances(new TerminateInstancesRequest(instancesIds));
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new TerminateInstancesResult();
  }

  public DescribeImagesResult decribeEc2Images(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, DescribeImagesRequest describeImagesRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonEC2Client amazonEc2Client = getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonEc2Client.describeImages(describeImagesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeImagesResult();
  }

  public List<String> listRegions(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonEC2Client amazonEC2Client =
          getAmazonEc2Client(Regions.US_EAST_1.getName(), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonEC2Client.describeRegions().getRegions().stream().map(Region::getRegionName).collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  public List<String> listVPCs(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonEC2Client amazonEC2Client = getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonEC2Client
          .describeVpcs(new DescribeVpcsRequest().withFilters(new Filter("state").withValues("available")))
          .getVpcs()
          .stream()
          .map(Vpc::getVpcId)
          .collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  public List<String> listSecurityGroupIds(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> vpcIds) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonEC2Client amazonEC2Client = getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
      List<Filter> filters = new ArrayList<>();
      if (isNotEmpty(vpcIds)) {
        filters.add(new Filter("vpc-id", vpcIds));
      }
      return amazonEC2Client.describeSecurityGroups(new DescribeSecurityGroupsRequest().withFilters(filters))
          .getSecurityGroups()
          .stream()
          .map(SecurityGroup::getGroupId)
          .collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  public List<String> listSubnetIds(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, List<String> vpcIds) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonEC2Client amazonEC2Client = getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
      List<Filter> filters = new ArrayList<>();
      if (isNotEmpty(vpcIds)) {
        filters.add(new Filter("vpc-id", vpcIds));
      }
      filters.add(new Filter("state").withValues("available"));
      return amazonEC2Client.describeSubnets(new DescribeSubnetsRequest().withFilters(filters))
          .getSubnets()
          .stream()
          .map(Subnet::getSubnetId)
          .collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  public Set<String> listTags(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    return listTags(awsConfig, encryptionDetails, region, ResourceType.Instance);
  }

  public Set<String> listTags(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, ResourceType resourceType) {
    String nextToken = null;
    Set<String> tags = new LinkedHashSet<>();
    try {
      do {
        encryptionService.decrypt(awsConfig, encryptionDetails);
        AmazonEC2Client amazonEC2Client =
            getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());

        DescribeTagsResult describeTagsResult = amazonEC2Client.describeTags(
            new DescribeTagsRequest()
                .withNextToken(nextToken)
                .withFilters(new Filter("resource-type").withValues(resourceType.toString()))
                .withMaxResults(1000));
        tags.addAll(describeTagsResult.getTags().stream().map(TagDescription::getKey).collect(Collectors.toSet()));
        nextToken = describeTagsResult.getNextToken();
      } while (nextToken != null);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return tags;
  }

  public List<AutoScalingGroup> listAutoScalingGroups(
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
      return result;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  public List<LaunchConfiguration> listLaunchConfigurations(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonAutoScalingClient amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());

      DescribeLaunchConfigurationsResult describeLaunchConfigurationsResult =
          amazonAutoScalingClient.describeLaunchConfigurations(
              new DescribeLaunchConfigurationsRequest().withMaxRecords(100));
      List<LaunchConfiguration> result = new ArrayList<>(describeLaunchConfigurationsResult.getLaunchConfigurations());
      while (isNotEmpty(describeLaunchConfigurationsResult.getNextToken())) {
        describeLaunchConfigurationsResult = amazonAutoScalingClient.describeLaunchConfigurations(
            new DescribeLaunchConfigurationsRequest().withMaxRecords(100).withNextToken(
                describeLaunchConfigurationsResult.getNextToken()));
        result.addAll(describeLaunchConfigurationsResult.getLaunchConfigurations());
      }
      return result;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  public LaunchConfiguration getLaunchConfiguration(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String launchConfigName) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonAutoScalingClient amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonAutoScalingClient
          .describeLaunchConfigurations(
              new DescribeLaunchConfigurationsRequest().withLaunchConfigurationNames(launchConfigName))
          .getLaunchConfigurations()
          .stream()
          .findFirst()
          .orElse(null);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return null;
  }

  public CreateClusterResult createCluster(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, CreateClusterRequest createClusterRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .createCluster(createClusterRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new CreateClusterResult();
  }

  public DescribeClustersResult describeClusters(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DescribeClustersRequest describeClustersRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeClusters(describeClustersRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeClustersResult();
  }

  public ListClustersResult listClusters(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, ListClustersRequest listClustersRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .listClusters(listClustersRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListClustersResult();
  }

  public RegisterTaskDefinitionResult registerTaskDefinition(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, RegisterTaskDefinitionRequest registerTaskDefinitionRequest) {
    encryptionService.decrypt(awsConfig, encryptionDetails);
    return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
        .registerTaskDefinition(registerTaskDefinitionRequest);
  }

  public ListServicesResult listServices(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, ListServicesRequest listServicesRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .listServices(listServicesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListServicesResult();
  }

  @SuppressFBWarnings("REC_CATCH_EXCEPTION")
  public DescribeServicesResult describeServices(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DescribeServicesRequest describeServicesRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeServices(describeServicesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeServicesResult();
  }

  public CreateServiceResult createService(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, CreateServiceRequest createServiceRequest) {
    encryptionService.decrypt(awsConfig, encryptionDetails);
    return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
        .createService(createServiceRequest);
  }

  public UpdateServiceResult updateService(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, UpdateServiceRequest updateServiceRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .updateService(updateServiceRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new UpdateServiceResult();
  }

  public DeleteServiceResult deleteService(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DeleteServiceRequest deleteServiceRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .deleteService(deleteServiceRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DeleteServiceResult();
  }

  public ListTasksResult listTasks(String region, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      ListTasksRequest listTasksRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey()).listTasks(listTasksRequest);
    } catch (ClusterNotFoundException ex) {
      throw new WingsException(ErrorCode.AWS_CLUSTER_NOT_FOUND).addParam("message", ex.getMessage());
    } catch (ServiceNotFoundException ex) {
      throw new WingsException(ErrorCode.AWS_SERVICE_NOT_FOUND).addParam("message", ex.getMessage());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListTasksResult();
  }

  public ListTaskDefinitionsResult listTaskDefinitions(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, ListTaskDefinitionsRequest listTaskDefinitionsRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .listTaskDefinitions(listTaskDefinitionsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListTaskDefinitionsResult();
  }

  public DescribeTaskDefinitionResult describeTaskDefinition(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DescribeTaskDefinitionRequest describeTaskDefinitionRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeTaskDefinition(describeTaskDefinitionRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeTaskDefinitionResult();
  }

  public DescribeTasksResult describeTasks(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DescribeTasksRequest describeTasksRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeTasks(describeTasksRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeTasksResult();
  }

  public DescribeTaskDefinitionResult describeTaskDefinitions(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DescribeTaskDefinitionRequest describeTaskDefinitionRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeTaskDefinition(describeTaskDefinitionRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeTaskDefinitionResult();
  }

  public DescribeContainerInstancesResult describeContainerInstances(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails,
      DescribeContainerInstancesRequest describeContainerInstancesRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeContainerInstances(describeContainerInstancesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeContainerInstancesResult();
  }

  public ListImagesResult listEcrImages(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      ListImagesRequest listImagesRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcrClient(awsConfig, region).listImages(listImagesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListImagesResult();
  }

  public ListImagesResult listEcrImages(
      EcrConfig ecrConfig, List<EncryptedDataDetail> encryptedDataDetails, ListImagesRequest listImagesRequest) {
    try {
      return getAmazonEcrClient(ecrConfig, encryptedDataDetails).listImages(listImagesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListImagesResult();
  }

  public DescribeRepositoriesResult listRepositories(EcrConfig ecrConfig,
      List<EncryptedDataDetail> encryptedDataDetails, DescribeRepositoriesRequest describeRepositoriesRequest) {
    try {
      return getAmazonEcrClient(ecrConfig, encryptedDataDetails).describeRepositories(describeRepositoriesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeRepositoriesResult();
  }

  public DescribeRepositoriesResult listRepositories(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      DescribeRepositoriesRequest describeRepositoriesRequest, String region) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcrClient(awsConfig, region).describeRepositories(describeRepositoriesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeRepositoriesResult();
  }

  public Repository getRepository(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String repositoryName) {
    DescribeRepositoriesRequest describeRepositoriesRequest = new DescribeRepositoriesRequest();
    describeRepositoriesRequest.setRepositoryNames(Lists.newArrayList(repositoryName));
    DescribeRepositoriesResult describeRepositoriesResult =
        listRepositories(awsConfig, encryptionDetails, describeRepositoriesRequest, region);
    List<Repository> repositories = describeRepositoriesResult.getRepositories();
    if (isNotEmpty(repositories)) {
      return repositories.get(0);
    }
    return null;
  }

  public List<LoadBalancerDescription> getLoadBalancerDescriptions(
      String region, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getClassicElbClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey())
          .describeLoadBalancers(
              new com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest().withPageSize(400))
          .getLoadBalancerDescriptions();
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  public List<TargetGroup> listTargetGroupsForAlb(
      String region, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String loadBalancerName) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient = getAmazonElasticLoadBalancingClient(
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

      DescribeTargetGroupsRequest describeTargetGroupsRequest = new DescribeTargetGroupsRequest().withPageSize(400);
      if (loadBalancerArn != null) {
        describeTargetGroupsRequest.withLoadBalancerArn(loadBalancerArn);
      }

      return amazonElasticLoadBalancingClient.describeTargetGroups(describeTargetGroupsRequest).getTargetGroups();
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  public TargetGroup getTargetGroupForAlb(
      String region, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String targetGroupArn) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient = getAmazonElasticLoadBalancingClient(
          Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());

      DescribeTargetGroupsRequest describeTargetGroupsRequest = new DescribeTargetGroupsRequest().withPageSize(5);
      describeTargetGroupsRequest.withTargetGroupArns(targetGroupArn);

      List<TargetGroup> targetGroupList =
          amazonElasticLoadBalancingClient.describeTargetGroups(describeTargetGroupsRequest).getTargetGroups();
      if (isNotEmpty(targetGroupList)) {
        return targetGroupList.get(0);
      }

    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }

    return null;
  }

  public void setAutoScalingGroupCapacityAndWaitForInstancesReadyState(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName, Integer desiredCapacity,
      ManagerExecutionLogCallback executionLogCallback, Integer autoScalingSteadyStateTimeout) {
    encryptionService.decrypt(awsConfig, encryptionDetails);
    AmazonAutoScalingClient amazonAutoScalingClient =
        getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
    try {
      executionLogCallback.saveExecutionLog(
          format("Set AutoScaling Group: [%s] desired capacity to [%s]", autoScalingGroupName, desiredCapacity));
      amazonAutoScalingClient.setDesiredCapacity(new SetDesiredCapacityRequest()
                                                     .withAutoScalingGroupName(autoScalingGroupName)
                                                     .withDesiredCapacity(desiredCapacity));
      executionLogCallback.saveExecutionLog("Successfully set desired capacity");
      waitForAllInstancesToBeReady(awsConfig, encryptionDetails, region, autoScalingGroupName, desiredCapacity,
          executionLogCallback, autoScalingSteadyStateTimeout);
    } catch (AmazonServiceException amazonServiceException) {
      describeAutoScalingGroupActivities(
          amazonAutoScalingClient, autoScalingGroupName, new HashSet<>(), executionLogCallback, true);
      handleAmazonServiceException(amazonServiceException);
    }
  }

  public void setAutoScalingGroupCapacityAndWaitForInstancesReadyState(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName, Integer desiredCapacity,
      ManagerExecutionLogCallback executionLogCallback) {
    setAutoScalingGroupCapacityAndWaitForInstancesReadyState(
        awsConfig, encryptionDetails, region, autoScalingGroupName, desiredCapacity, executionLogCallback, 10);
  }

  public AttachLoadBalancersResult attachLoadBalancerToAutoScalingGroup(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region,
      AttachLoadBalancersRequest attachLoadBalancersRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonAutoScalingClient amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonAutoScalingClient.attachLoadBalancers(attachLoadBalancersRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new AttachLoadBalancersResult();
  }

  public AttachLoadBalancerTargetGroupsResult attachTargetGroupsToAutoScalingGroup(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region,
      AttachLoadBalancerTargetGroupsRequest attachLoadBalancerTargetGroupsRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonAutoScalingClient amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonAutoScalingClient.attachLoadBalancerTargetGroups(attachLoadBalancerTargetGroupsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new AttachLoadBalancerTargetGroupsResult();
  }

  public AutoScalingGroup getAutoScalingGroup(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName) {
    try {
      DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult =
          describeAutoScalingGroups(awsConfig, encryptionDetails, region,
              new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(autoScalingGroupName));
      return describeAutoScalingGroupsResult.getAutoScalingGroups().isEmpty()
          ? null
          : describeAutoScalingGroupsResult.getAutoScalingGroups().get(0);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return null;
  }

  private void waitForAllInstancesToBeReady(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String autoScalingGroupName, Integer desiredCount,
      ManagerExecutionLogCallback executionLogCallback, Integer autoScalingSteadyStateTimeout) {
    try {
      timeLimiter.callWithTimeout(() -> {
        AmazonAutoScalingClient amazonAutoScalingClient =
            getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
        Set<String> completedActivities = new HashSet<>();
        while (true) {
          List<String> instanceIds =
              listInstanceIdsFromAutoScalingGroup(awsConfig, encryptionDetails, region, autoScalingGroupName);
          describeAutoScalingGroupActivities(
              amazonAutoScalingClient, autoScalingGroupName, completedActivities, executionLogCallback, false);

          if (instanceIds.size() == desiredCount
              && allInstanceInReadyState(awsConfig, encryptionDetails, region, instanceIds, executionLogCallback)) {
            return true;
          }
          sleep(ofSeconds(AUTOSCALING_REQUEST_STATUS_CHECK_INTERVAL));
        }
      }, autoScalingSteadyStateTimeout, TimeUnit.MINUTES, true);
    } catch (UncheckedTimeoutException e) {
      executionLogCallback.saveExecutionLog(
          "Request timeout. AutoScaling group couldn't reach steady state", CommandExecutionStatus.FAILURE);
      throw new WingsException(INIT_TIMEOUT)
          .addParam("message", "Timed out waiting for all instances to be in running state");
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException("Error while waiting for all instances to be in running state", e);
    }
    executionLogCallback.saveExecutionLog("AutoScaling group reached steady state");
  }

  private boolean allInstanceInReadyState(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, List<String> instanceIds, ManagerExecutionLogCallback executionLogCallback) {
    DescribeInstancesResult describeInstancesResult = describeEc2Instances(
        awsConfig, encryptionDetails, region, new DescribeInstancesRequest().withInstanceIds(instanceIds));
    boolean allRunning = instanceIds.isEmpty()
        || describeInstancesResult.getReservations()
               .stream()
               .flatMap(reservation -> reservation.getInstances().stream())
               .allMatch(instance -> instance.getState().getName().equals("running"));
    if (!allRunning) {
      Map<String, Long> instanceStateCountMap =
          describeInstancesResult.getReservations()
              .stream()
              .flatMap(reservation -> reservation.getInstances().stream())
              .collect(groupingBy(instance -> instance.getState().getName(), counting()));
      executionLogCallback.saveExecutionLog("Waiting for instances to be in running state. "
          + Joiner.on(",").withKeyValueSeparator("=").join(instanceStateCountMap));
    }
    return allRunning;
  }

  public DescribeInstancesResult describeAutoScalingGroupInstances(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonEC2Client amazonEc2Client = getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
      List<String> instanceIds =
          listInstanceIdsFromAutoScalingGroup(awsConfig, encryptionDetails, region, autoScalingGroupName);
      return instanceIds.isEmpty()
          ? new DescribeInstancesResult()
          : amazonEc2Client.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceIds));
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeInstancesResult();
  }

  public List<Instance> listAutoScalingGroupInstances(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName) {
    List<Instance> instanceList = Lists.newArrayList();
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonEC2Client amazonEc2Client = getAmazonEc2Client(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
      List<String> instanceIds =
          listInstanceIdsFromAutoScalingGroup(awsConfig, encryptionDetails, region, autoScalingGroupName);

      if (CollectionUtils.isEmpty(instanceIds)) {
        return instanceList;
      }

      // This will return only RUNNING instances
      DescribeInstancesRequest describeInstancesRequest =
          getDescribeInstancesRequestWithRunningFilter().withInstanceIds(instanceIds);
      DescribeInstancesResult describeInstancesResult;

      do {
        describeInstancesResult = amazonEc2Client.describeInstances(describeInstancesRequest);
        describeInstancesResult.getReservations().stream().forEach(
            reservation -> instanceList.addAll(reservation.getInstances()));

        describeInstancesRequest.withNextToken(describeInstancesResult.getNextToken());

      } while (describeInstancesResult.getNextToken() != null);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return instanceList;
  }

  public List<String> listInstanceIdsFromAutoScalingGroup(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName) {
    return getAutoScalingGroup(awsConfig, encryptionDetails, region, autoScalingGroupName)
        .getInstances()
        .stream()
        .map(com.amazonaws.services.autoscaling.model.Instance::getInstanceId)
        .collect(toList());
  }

  List<String> listIAMInstanceRoles(AwsConfig awsConfig) {
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
    return emptyList();
  }

  Map<String, String> listIAMRoles(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonIdentityManagementClient amazonIdentityManagementClient =
          getAmazonIdentityManagementClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());

      return amazonIdentityManagementClient.listRoles(new ListRolesRequest().withMaxItems(400))
          .getRoles()
          .stream()
          .collect(Collectors.toMap(Role::getArn, Role::getRoleName));
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyMap();
  }

  List<String> listApplicationLoadBalancers(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient = getAmazonElasticLoadBalancingClient(
          Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonElasticLoadBalancingClient
          .describeLoadBalancers(new DescribeLoadBalancersRequest().withPageSize(400))
          .getLoadBalancers()
          .stream()
          .filter(loadBalancer -> StringUtils.equalsIgnoreCase(loadBalancer.getType(), "application"))
          .map(LoadBalancer::getLoadBalancerName)
          .collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  List<String> listClassicLoadBalancers(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      List<LoadBalancerDescription> describeLoadBalancers =
          getLoadBalancerDescriptions(region, awsConfig, encryptionDetails);
      return describeLoadBalancers.stream().map(LoadBalancerDescription::getLoadBalancerName).collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  public CreateLaunchConfigurationResult createLaunchConfiguration(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region,
      CreateLaunchConfigurationRequest createLaunchConfigurationRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonAutoScalingClient amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonAutoScalingClient.createLaunchConfiguration(createLaunchConfigurationRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new CreateLaunchConfigurationResult();
  }

  public CreateAutoScalingGroupResult createAutoScalingGroup(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region,
      CreateAutoScalingGroupRequest createAutoScalingGroupRequest, LogCallback logCallback) {
    AmazonAutoScalingClient amazonAutoScalingClient = null;
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonAutoScalingClient.createAutoScalingGroup(createAutoScalingGroupRequest);
    } catch (AmazonServiceException amazonServiceException) {
      if (amazonAutoScalingClient != null && logCallback != null) {
        describeAutoScalingGroupActivities(amazonAutoScalingClient,
            createAutoScalingGroupRequest.getAutoScalingGroupName(), new HashSet<>(), logCallback, true);
      }
      handleAmazonServiceException(amazonServiceException);
    }
    return new CreateAutoScalingGroupResult();
  }

  public DescribeAutoScalingGroupsResult describeAutoScalingGroups(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region,
      DescribeAutoScalingGroupsRequest autoScalingGroupsRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonAutoScalingClient amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return amazonAutoScalingClient.describeAutoScalingGroups(autoScalingGroupsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeAutoScalingGroupsResult();
  }

  public AutoScalingGroup getAutoScalingGroups(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName) {
    try {
      DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult =
          describeAutoScalingGroups(awsConfig, encryptionDetails, region,
              new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(autoScalingGroupName));
      return describeAutoScalingGroupsResult.getAutoScalingGroups().isEmpty()
          ? null
          : describeAutoScalingGroupsResult.getAutoScalingGroups().get(0);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return null;
  }

  public void deleteLaunchConfig(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonAutoScalingClient amazonAutoScalingClient =
          getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      amazonAutoScalingClient.deleteLaunchConfiguration(
          new DeleteLaunchConfigurationRequest().withLaunchConfigurationName(autoScalingGroupName));
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
  }

  public void deleteAutoScalingGroups(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      List<AutoScalingGroup> autoScalingGroups, LogCallback callback) {
    AmazonAutoScalingClient amazonAutoScalingClient =
        getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      autoScalingGroups.forEach(autoScalingGroup -> {
        try {
          amazonAutoScalingClient.deleteAutoScalingGroup(
              new DeleteAutoScalingGroupRequest().withAutoScalingGroupName(autoScalingGroup.getAutoScalingGroupName()));
          waitForAutoScalingGroupToBeDeleted(amazonAutoScalingClient, autoScalingGroup, callback);
        } catch (Exception ignored) {
          describeAutoScalingGroupActivities(
              amazonAutoScalingClient, autoScalingGroup.getAutoScalingGroupName(), new HashSet<>(), callback, true);
          logger.warn("Failed to delete ASG: [{}] [{}]", autoScalingGroup.getAutoScalingGroupName(), ignored);
        }
        try {
          amazonAutoScalingClient.deleteLaunchConfiguration(
              new DeleteLaunchConfigurationRequest().withLaunchConfigurationName(
                  autoScalingGroup.getLaunchConfigurationName()));
        } catch (Exception ignored) {
          describeAutoScalingGroupActivities(
              amazonAutoScalingClient, autoScalingGroup.getAutoScalingGroupName(), new HashSet<>(), callback, true);
          logger.warn("Failed to delete ASG: [{}] [{}]", autoScalingGroup.getAutoScalingGroupName(), ignored);
        }
      });
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
  }

  private void waitForAutoScalingGroupToBeDeleted(
      AmazonAutoScalingClient amazonAutoScalingClient, AutoScalingGroup autoScalingGroup, LogCallback callback) {
    try {
      timeLimiter.callWithTimeout(() -> {
        Set<String> completedActivities = new HashSet<>();
        while (true) {
          DescribeAutoScalingGroupsResult result = amazonAutoScalingClient.describeAutoScalingGroups(
              new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(
                  autoScalingGroup.getAutoScalingGroupName()));
          if (result.getAutoScalingGroups().isEmpty()) {
            return true;
          }
          describeAutoScalingGroupActivities(amazonAutoScalingClient, autoScalingGroup.getAutoScalingGroupName(),
              completedActivities, callback, false);
          sleep(ofSeconds(AUTOSCALING_REQUEST_STATUS_CHECK_INTERVAL));
        }
      }, 1L, TimeUnit.MINUTES, true);
    } catch (UncheckedTimeoutException e) {
      throw new WingsException(INIT_TIMEOUT)
          .addParam("message", "Timed out waiting for autoscaling group to be deleted");
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException("Error while waiting for autoscaling group to be deleted", e);
    }
  }

  protected void describeAutoScalingGroupActivities(AmazonAutoScalingClient amazonAutoScalingClient,
      String autoScalingGroupName, Set<String> completedActivities, LogCallback callback, boolean withCause) {
    if (callback == null) {
      logger.info("Not describing autoScalingGroupActivities for [%s] since logCallback is null");
      return;
    }
    try {
      DescribeScalingActivitiesResult activitiesResult = amazonAutoScalingClient.describeScalingActivities(
          new DescribeScalingActivitiesRequest().withAutoScalingGroupName(autoScalingGroupName));
      List<Activity> activities = activitiesResult.getActivities();
      if (activities != null && activities.size() > 0) {
        activities.stream()
            .filter(activity -> !completedActivities.contains(activity.getActivityId()))
            .forEach(activity -> {
              String activityId = activity.getActivityId();
              String details = activity.getDetails();
              Integer progress = activity.getProgress();
              String activityDescription = activity.getDescription();
              String statuscode = activity.getStatusCode();
              String statusMessage = activity.getStatusMessage();
              String logStatement =
                  format("AutoScalingGroup [%s] activity [%s] progress [%d percent] , statuscode [%s]  details [%s]",
                      autoScalingGroupName, activityDescription, progress, statuscode, details);
              if (withCause) {
                String cause = activity.getCause();
                logStatement = format(logStatement + " cause [%s]", cause);
              }

              callback.saveExecutionLog(logStatement);
              if (progress == 100) {
                completedActivities.add(activityId);
              }
            });
      }
    } catch (Exception e) {
      logger.warn("Failed to describe autoScalingGroup for [%s]", autoScalingGroupName, e);
    }
  }

  public Datapoint getCloudWatchMetricStatistics(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, GetMetricStatisticsRequest metricStatisticsRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonCloudWatchClient cloudWatchClient =
          getAwsCloudWatchClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());
      return cloudWatchClient.getMetricStatistics(metricStatisticsRequest)
          .getDatapoints()
          .stream()
          .max(comparing(Datapoint::getTimestamp))
          .orElse(null);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new Datapoint();
  }

  public List<Metric> getCloudWatchMetrics(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonCloudWatchClient cloudWatchClient =
          getAwsCloudWatchClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());

      List<Metric> rv = new ArrayList<>();
      String nextToken = null;
      do {
        ListMetricsRequest request = new ListMetricsRequest();
        request.withNextToken(nextToken);
        ListMetricsResult listMetricsResult = cloudWatchClient.listMetrics(request);
        nextToken = listMetricsResult.getNextToken();
        rv.addAll(listMetricsResult.getMetrics());
      } while (nextToken != null);

      return rv;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  public List<Metric> getCloudWatchMetrics(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, ListMetricsRequest listMetricsRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonCloudWatchClient cloudWatchClient =
          getAwsCloudWatchClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey());

      List<Metric> rv = new ArrayList<>();
      String nextToken = null;
      do {
        listMetricsRequest.withNextToken(nextToken);
        ListMetricsResult listMetricsResult = cloudWatchClient.listMetrics(listMetricsRequest);
        nextToken = listMetricsResult.getNextToken();
        rv.addAll(listMetricsResult.getMetrics());
      } while (nextToken != null);

      return rv;

    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
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

  public CreateStackResult createStack(
      String region, String accessKey, char[] secretKey, CreateStackRequest createStackRequest) {
    try {
      AmazonCloudFormationClient cloudFormationClient =
          getAmazonCloudFormationClient(Regions.fromName(region), accessKey, secretKey);
      return cloudFormationClient.createStack(createStackRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new CreateStackResult();
  }

  public UpdateStackResult updateStack(
      String region, String accessKey, char[] secretKey, UpdateStackRequest updateStackRequest) {
    try {
      AmazonCloudFormationClient cloudFormationClient =
          getAmazonCloudFormationClient(Regions.fromName(region), accessKey, secretKey);
      return cloudFormationClient.updateStack(updateStackRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new UpdateStackResult();
  }

  public DescribeStacksResult describeStacks(
      String region, String accessKey, char[] secretKey, DescribeStacksRequest describeStacksRequest) {
    try {
      AmazonCloudFormationClient cloudFormationClient =
          getAmazonCloudFormationClient(Regions.fromName(region), accessKey, secretKey);
      return cloudFormationClient.describeStacks(describeStacksRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new DescribeStacksResult();
  }

  public List<Stack> getAllStacks(
      String region, String accessKey, char[] secretKey, DescribeStacksRequest describeStacksRequest) {
    AmazonCloudFormationClient cloudFormationClient =
        getAmazonCloudFormationClient(Regions.fromName(region), accessKey, secretKey);
    try {
      List<Stack> stacks = new ArrayList<>();
      String nextToken = null;
      do {
        describeStacksRequest.withNextToken(nextToken);
        DescribeStacksResult result = cloudFormationClient.describeStacks(describeStacksRequest);
        nextToken = result.getNextToken();
        stacks.addAll(result.getStacks());
      } while (nextToken != null);
      return stacks;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  public List<StackEvent> getAllStackEvents(
      String region, String accessKey, char[] secretKey, DescribeStackEventsRequest describeStackEventsRequest) {
    AmazonCloudFormationClient cloudFormationClient =
        getAmazonCloudFormationClient(Regions.fromName(region), accessKey, secretKey);
    try {
      List<StackEvent> stacksEvents = new ArrayList<>();
      String nextToken = null;
      do {
        describeStackEventsRequest.withNextToken(nextToken);
        DescribeStackEventsResult result = cloudFormationClient.describeStackEvents(describeStackEventsRequest);
        nextToken = result.getNextToken();
        stacksEvents.addAll(result.getStackEvents());
      } while (nextToken != null);
      return stacksEvents;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  public void deleteStack(String region, String accessKey, char[] secretKey, DeleteStackRequest deleteStackRequest) {
    try {
      AmazonCloudFormationClient cloudFormationClient =
          getAmazonCloudFormationClient(Regions.fromName(region), accessKey, secretKey);
      cloudFormationClient.deleteStack(deleteStackRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
  }

  public ListFunctionsResult listFunctions(
      String region, String accessKey, char[] secretKey, ListFunctionsRequest listFunctionsRequest) {
    try {
      return getAmazonLambdaClient(region, accessKey, secretKey).listFunctions(listFunctionsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListFunctionsResult();
  }

  public GetFunctionResult getFunction(
      String region, String accessKey, char[] secretKey, GetFunctionRequest getFunctionRequest) {
    try {
      return getAmazonLambdaClient(region, accessKey, secretKey).getFunction(getFunctionRequest);
    } catch (ResourceNotFoundException exception) {
      return null;
    } catch (AmazonServiceException exception) {
      handleAmazonServiceException(exception);
    }
    return new GetFunctionResult();
  }

  public ListVersionsByFunctionResult listVersionsByFunction(
      String region, String accessKey, char[] secretKey, ListVersionsByFunctionRequest listVersionsByFunctionRequest) {
    try {
      return getAmazonLambdaClient(region, accessKey, secretKey).listVersionsByFunction(listVersionsByFunctionRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListVersionsByFunctionResult();
  }

  public CreateFunctionResult createFunction(
      String region, String accessKey, char[] secretKey, CreateFunctionRequest createFunctionRequest) {
    try {
      return getAmazonLambdaClient(region, accessKey, secretKey).createFunction(createFunctionRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new CreateFunctionResult();
  }

  public UpdateFunctionCodeResult updateFunctionCode(
      String region, String accessKey, char[] secretKey, UpdateFunctionCodeRequest updateFunctionCodeRequest) {
    try {
      return getAmazonLambdaClient(region, accessKey, secretKey).updateFunctionCode(updateFunctionCodeRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new UpdateFunctionCodeResult();
  }

  public UpdateFunctionConfigurationResult updateFunctionConfiguration(String region, String accessKey,
      char[] secretKey, UpdateFunctionConfigurationRequest updateFunctionConfigurationRequest) {
    try {
      return getAmazonLambdaClient(region, accessKey, secretKey)
          .updateFunctionConfiguration(updateFunctionConfigurationRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new UpdateFunctionConfigurationResult();
  }

  public PublishVersionResult publishVersion(
      String region, String accessKey, char[] secretKey, PublishVersionRequest publishVersionRequest) {
    try {
      return getAmazonLambdaClient(region, accessKey, secretKey).publishVersion(publishVersionRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new PublishVersionResult();
  }

  public ListAliasesResult listAliases(
      String region, String accessKey, char[] secretKey, ListAliasesRequest listAliasesRequest) {
    try {
      return getAmazonLambdaClient(region, accessKey, secretKey).listAliases(listAliasesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new ListAliasesResult();
  }

  public CreateAliasResult createAlias(
      String region, String accessKey, char[] secretKey, CreateAliasRequest createAliasRequest) {
    try {
      return getAmazonLambdaClient(region, accessKey, secretKey).createAlias(createAliasRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new CreateAliasResult();
  }

  public UpdateAliasResult updateAlias(
      String region, String accessKey, char[] secretKey, UpdateAliasRequest updateAliasRequest) {
    try {
      return getAmazonLambdaClient(region, accessKey, secretKey).updateAlias(updateAliasRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new UpdateAliasResult();
  }

  public InvokeResult invokeFunction(String region, String accessKey, char[] secretKey, InvokeRequest invokeRequest) {
    try {
      return getAmazonLambdaClient(region, accessKey, secretKey).invoke(invokeRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return new InvokeResult();
  }

  public boolean isVersioningEnabledForBucket(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      BucketVersioningConfiguration bucketVersioningConfiguration = getAmazonS3Client(
          awsConfig.getAccessKey(), awsConfig.getSecretKey(), getBucketRegion(awsConfig, encryptionDetails, bucketName))
                                                                        .getBucketVersioningConfiguration(bucketName);
      return "ENABLED".equals(bucketVersioningConfiguration.getStatus());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return false;
  }

  private String getBucketRegion(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      // You can query the bucket location using any region, it returns the result. So, using the default
      String region = getAmazonS3Client(awsConfig.getAccessKey(), awsConfig.getSecretKey(), "us-east-1")
                          .getBucketLocation(bucketName);
      // Aws returns US if the bucket was created in the default region. Not sure why it doesn't return just the region
      // name in all cases. Also, their documentation says it would return empty string if its in the default region.
      // http://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketGETlocation.html But it returns US. Added additional
      // checks based on other stuff
      if (region == null || region.equals("US")) {
        return "us-east-1";
      } else if (region.equals("EU")) {
        return "eu-west-1";
      }

      return region;
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return null;
  }

  @SuppressFBWarnings("REC_CATCH_EXCEPTION")
  public static boolean isInAwsRegion(String region) {
    try {
      HttpEntity entity =
          HttpClients.custom()
              .setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(2000).setSocketTimeout(2000).build())
              .build()
              .execute(new HttpGet(AWS_AVAILABILITY_ZONE_CHECK))
              .getEntity();
      String availabilityZone =
          entity != null ? EntityUtils.toString(entity, ContentType.getOrDefault(entity).getCharset()) : "none";
      return startsWith(availabilityZone, region);
    } catch (Exception e) {
      return false;
    }
  }

  public List<Filter> getAwsFilters(AwsInfrastructureMapping awsInfrastructureMapping) {
    AwsInstanceFilter instanceFilter = awsInfrastructureMapping.getAwsInstanceFilter();
    List<Filter> filters = new ArrayList<>();
    filters.add(new Filter("instance-state-name").withValues("running"));
    if (instanceFilter != null) {
      if (isNotEmpty(instanceFilter.getVpcIds())) {
        filters.add(new Filter("vpc-id", instanceFilter.getVpcIds()));
      }
      if (isNotEmpty(instanceFilter.getSecurityGroupIds())) {
        filters.add(new Filter("instance.group-id", instanceFilter.getSecurityGroupIds()));
      }
      if (isNotEmpty(instanceFilter.getSubnetIds())) {
        filters.add(new Filter("network-interface.subnet-id", instanceFilter.getSubnetIds()));
      }
      if (isNotEmpty(instanceFilter.getTags())) {
        Multimap<String, String> tags = ArrayListMultimap.create();
        instanceFilter.getTags().forEach(tag -> tags.put(tag.getKey(), tag.getValue()));
        tags.keySet().forEach(key -> filters.add(new Filter("tag:" + key, new ArrayList<>(tags.get(key)))));
      }
      if (StringUtils.equals(awsInfrastructureMapping.getDeploymentType(), DeploymentType.WINRM.toString())) {
        filters.add(new Filter("platform", asList("windows")));
      }
    }
    return filters;
  }

  public List<Filter> getAwsFiltersForRunningState() {
    return getAwsFilters(AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping().build());
  }

  public DescribeInstancesRequest getDescribeInstancesRequestWithRunningFilter() {
    return new DescribeInstancesRequest().withFilters(getAwsFiltersForRunningState());
  }
}
