package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INIT_TIMEOUT;
import static io.harness.exception.WingsException.USER;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.startsWith;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.Activity;
import com.amazonaws.services.autoscaling.model.AmazonAutoScalingException;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupResult;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesRequest;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesResult;
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
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ec2.model.ResourceType;
import com.amazonaws.services.ec2.model.TagDescription;
import com.amazonaws.services.ecr.AmazonECRClient;
import com.amazonaws.services.ecr.AmazonECRClientBuilder;
import com.amazonaws.services.ecr.model.AmazonECRException;
import com.amazonaws.services.ecr.model.DescribeRepositoriesRequest;
import com.amazonaws.services.ecr.model.DescribeRepositoriesResult;
import com.amazonaws.services.ecr.model.GetAuthorizationTokenRequest;
import com.amazonaws.services.ecr.model.ListImagesRequest;
import com.amazonaws.services.ecr.model.ListImagesResult;
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
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketVersioningConfiguration;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.EcrConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.LogCallback;
import software.wings.common.Constants;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.states.ManagerExecutionLogCallback;
import software.wings.utils.Misc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
  @Inject private ManagerExpressionEvaluator expressionEvaluator;
  @Inject private AwsUtils awsUtils;

  private static final long AUTOSCALING_REQUEST_STATUS_CHECK_INTERVAL = TimeUnit.SECONDS.toSeconds(15);

  private static final Logger logger = LoggerFactory.getLogger(AwsHelperService.class);

  public boolean validateAwsAccountCredential(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      getAmazonEc2Client(Regions.US_EAST_1.getName(), awsConfig.getAccessKey(), awsConfig.getSecretKey(),
          awsConfig.isUseEc2IamCredentials())
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
      getAmazonEc2Client(Regions.US_EAST_1.getName(), accessKey, secretKey, false).describeRegions();
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
  private AmazonCloudWatchClient getAwsCloudWatchClient(
      String region, String accessKey, char[] secretKey, boolean useEc2IamCredentials) {
    AmazonCloudWatchClientBuilder builder = AmazonCloudWatchClientBuilder.standard().withRegion(region);
    attachCredentials(builder, useEc2IamCredentials, accessKey, secretKey);
    return (AmazonCloudWatchClient) builder.build();
  }

  /**
   * Gets amazon ecs client.
   *
   * @param region    the region
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon ecs client
   */
  private AmazonECSClient getAmazonEcsClient(
      String region, String accessKey, char[] secretKey, boolean useEc2IamCredentials) {
    AmazonECSClientBuilder builder = AmazonECSClientBuilder.standard().withRegion(region);
    attachCredentials(builder, useEc2IamCredentials, accessKey, secretKey);
    return (AmazonECSClient) builder.build();
  }

  private AmazonECRClient getAmazonEcrClient(AwsConfig awsConfig, String region) {
    AmazonECRClientBuilder builder = AmazonECRClientBuilder.standard().withRegion(region);
    attachCredentials(builder, awsConfig.isUseEc2IamCredentials(), awsConfig.getAccessKey(), awsConfig.getSecretKey());
    return (AmazonECRClient) builder.build();
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

  /**
   * Gets amazon s3 client.
   *
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon s3 client
   */
  private AmazonS3Client getAmazonS3Client(
      String accessKey, char[] secretKey, String region, boolean useEc2IamCredentials) {
    AmazonS3ClientBuilder builder =
        AmazonS3ClientBuilder.standard().withRegion(region).withForceGlobalBucketAccessEnabled(true);
    attachCredentials(builder, useEc2IamCredentials, accessKey, secretKey);
    return (AmazonS3Client) builder.build();
  }

  private void attachCredentials(
      AwsClientBuilder builder, boolean useEc2IamCredentials, String accessKey, char[] secretKey) {
    if (useEc2IamCredentials) {
      builder.withCredentials(InstanceProfileCredentialsProvider.getInstance());
    } else {
      builder.withCredentials(
          new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))));
    }
  }

  /**
   * Gets amazon ec 2 client.
   *
   * @param region    the region
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon ec 2 client
   */
  private AmazonEC2Client getAmazonEc2Client(
      String region, String accessKey, char[] secretKey, boolean useEc2IamCredentials) {
    AmazonEC2ClientBuilder builder = AmazonEC2ClientBuilder.standard().withRegion(region);
    attachCredentials(builder, useEc2IamCredentials, accessKey, secretKey);
    return (AmazonEC2Client) builder.build();
  }

  /**
   * Gets amazon code deploy client.
   *
   * @param region    the region
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon code deploy client
   */
  private AmazonCodeDeployClient getAmazonCodeDeployClient(
      Regions region, String accessKey, char[] secretKey, boolean useEc2IamCredentials) {
    AmazonCodeDeployClientBuilder builder = AmazonCodeDeployClientBuilder.standard().withRegion(region);
    attachCredentials(builder, useEc2IamCredentials, accessKey, secretKey);
    return (AmazonCodeDeployClient) builder.build();
  }

  @VisibleForTesting
  AmazonCloudFormationClient getAmazonCloudFormationClient(
      Regions region, String accessKey, char[] secretKey, boolean useEc2IamCredentials) {
    AmazonCloudFormationClientBuilder builder = AmazonCloudFormationClientBuilder.standard().withRegion(region);
    attachCredentials(builder, useEc2IamCredentials, accessKey, secretKey);
    return (AmazonCloudFormationClient) builder.build();
  }

  /**
   * Gets amazon auto scaling client.
   *
   * @param region    the region
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon auto scaling client
   */
  private AmazonAutoScalingClient getAmazonAutoScalingClient(
      Regions region, String accessKey, char[] secretKey, boolean useEc2IamCredentials) {
    AmazonAutoScalingClientBuilder builder = AmazonAutoScalingClientBuilder.standard().withRegion(region);
    attachCredentials(builder, useEc2IamCredentials, accessKey, secretKey);
    return (AmazonAutoScalingClient) builder.build();
  }

  /**
   * Gets amazon elastic load balancing client.
   *
   * @param accessKey the access key
   * @param secretKey the secret key
   * @return the amazon elastic load balancing client
   */
  private AmazonElasticLoadBalancingClient getAmazonElasticLoadBalancingClient(
      Regions region, String accessKey, char[] secretKey, boolean useEc2IamCredentials) {
    com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder builder =
        com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder.standard().withRegion(
            region);
    attachCredentials(builder, useEc2IamCredentials, accessKey, secretKey);
    return (AmazonElasticLoadBalancingClient) builder.build();
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
      Regions region, String accessKey, char[] secretKey, boolean useEc2IamCredentials) {
    AmazonElasticLoadBalancingClientBuilder builder =
        AmazonElasticLoadBalancingClientBuilder.standard().withRegion(region);
    attachCredentials(builder, useEc2IamCredentials, accessKey, secretKey);
    return (com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient) builder.build();
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
  public String getInstanceId(
      Regions region, String accessKey, char[] secretKey, String hostName, boolean useEc2IamCredentials) {
    AmazonEC2Client amazonEC2Client = getAmazonEc2Client(region.getName(), accessKey, secretKey, useEc2IamCredentials);

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

  public List<Bucket> listS3Buckets(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonS3Client(
          awsConfig.getAccessKey(), awsConfig.getSecretKey(), "us-east-1", awsConfig.isUseEc2IamCredentials())
          .listBuckets();
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return Collections.emptyList();
  }

  public S3Object getObjectFromS3(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName, String key) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonS3Client(awsConfig.getAccessKey(), awsConfig.getSecretKey(),
          getBucketRegion(awsConfig, encryptionDetails, bucketName), awsConfig.isUseEc2IamCredentials())
          .getObject(bucketName, key);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return null;
  }

  public ObjectMetadata getObjectMetadataFromS3(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName, String key) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonS3Client(awsConfig.getAccessKey(), awsConfig.getSecretKey(),
          getBucketRegion(awsConfig, encryptionDetails, bucketName), awsConfig.isUseEc2IamCredentials())
          .getObjectMetadata(bucketName, key);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return null;
  }

  public ListObjectsV2Result listObjectsInS3(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, ListObjectsV2Request listObjectsV2Request) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonS3Client amazonS3Client = getAmazonS3Client(awsConfig.getAccessKey(), awsConfig.getSecretKey(),
          getBucketRegion(awsConfig, encryptionDetails, listObjectsV2Request.getBucketName()),
          awsConfig.isUseEc2IamCredentials());
      return amazonS3Client.listObjectsV2(listObjectsV2Request);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new ListObjectsV2Result();
  }

  public AwsConfig validateAndGetAwsConfig(
      SettingAttribute connectorConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    if (connectorConfig == null || connectorConfig.getValue() == null
        || !(connectorConfig.getValue() instanceof AwsConfig)) {
      throw new InvalidRequestException("connectorConfig is not of type AwsConfig");
    }
    encryptionService.decrypt((EncryptableSetting) connectorConfig.getValue(), encryptedDataDetails);
    return (AwsConfig) connectorConfig.getValue();
  }

  private void handleAmazonClientException(AmazonClientException amazonClientException) {
    logger.error("AWS API Client call exception", amazonClientException);
    String errorMessage = amazonClientException.getMessage();
    if (isNotEmpty(errorMessage) && errorMessage.contains("/meta-data/iam/security-credentials/")) {
      throw new InvalidRequestException("The IAM role on the Ec2 delegate does not exist OR does not"
              + " have required permissions.",
          amazonClientException, USER);
    } else {
      logger.error("Unhandled aws exception");
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED).addParam("message", amazonClientException.getMessage());
    }
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
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey(),
          awsConfig.isUseEc2IamCredentials())
          .listDeploymentGroups(listDeploymentGroupsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new ListDeploymentGroupsResult();
  }

  public ListApplicationsResult listApplicationsResult(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, ListApplicationsRequest listApplicationsRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey(),
          awsConfig.isUseEc2IamCredentials())
          .listApplications(listApplicationsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new ListApplicationsResult();
  }

  public ListDeploymentConfigsResult listDeploymentConfigsResult(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region,
      ListDeploymentConfigsRequest listDeploymentConfigsRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey(),
          awsConfig.isUseEc2IamCredentials())
          .listDeploymentConfigs(listDeploymentConfigsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new ListDeploymentConfigsResult();
  }

  public GetDeploymentResult getCodeDeployDeployment(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, GetDeploymentRequest getDeploymentRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey(),
          awsConfig.isUseEc2IamCredentials())
          .getDeployment(getDeploymentRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new GetDeploymentResult();
  }

  public GetDeploymentGroupResult getCodeDeployDeploymentGroup(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region, GetDeploymentGroupRequest getDeploymentGroupRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey(),
          awsConfig.isUseEc2IamCredentials())
          .getDeploymentGroup(getDeploymentGroupRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new GetDeploymentGroupResult();
  }

  public CreateDeploymentResult createCodeDeployDeployment(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region, CreateDeploymentRequest createDeploymentRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey(),
          awsConfig.isUseEc2IamCredentials())
          .createDeployment(createDeploymentRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new CreateDeploymentResult();
  }

  public ListDeploymentInstancesResult listDeploymentInstances(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region,
      ListDeploymentInstancesRequest listDeploymentInstancesRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey(),
          awsConfig.isUseEc2IamCredentials())
          .listDeploymentInstances(listDeploymentInstancesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
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
      return getAmazonEc2Client(
          region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials())
          .describeInstances(describeInstancesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new DescribeInstancesResult();
  }

  public DescribeImagesResult decribeEc2Images(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, DescribeImagesRequest describeImagesRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonEC2Client amazonEc2Client = getAmazonEc2Client(
          region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials());
      return amazonEc2Client.describeImages(describeImagesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new DescribeImagesResult();
  }

  public List<String> listRegions(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonEC2Client amazonEC2Client = getAmazonEc2Client(Regions.US_EAST_1.getName(), awsConfig.getAccessKey(),
          awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials());
      return amazonEC2Client.describeRegions().getRegions().stream().map(Region::getRegionName).collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  public Set<String> listTags(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    return listTags(awsConfig, encryptionDetails, region, ResourceType.Instance);
  }

  public Set<String> listTags(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, ResourceType resourceType) {
    String nextToken = null;
    Set<String> tags = new HashSet<>();
    try {
      do {
        encryptionService.decrypt(awsConfig, encryptionDetails);
        AmazonEC2Client amazonEC2Client = getAmazonEc2Client(
            region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials());

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
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return tags;
  }

  public CreateClusterResult createCluster(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, CreateClusterRequest createClusterRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(
          region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials())
          .createCluster(createClusterRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new CreateClusterResult();
  }

  public DescribeClustersResult describeClusters(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DescribeClustersRequest describeClustersRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(
          region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials())
          .describeClusters(describeClustersRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new DescribeClustersResult();
  }

  public ListClustersResult listClusters(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, ListClustersRequest listClustersRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(
          region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials())
          .listClusters(listClustersRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new ListClustersResult();
  }

  public RegisterTaskDefinitionResult registerTaskDefinition(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, RegisterTaskDefinitionRequest registerTaskDefinitionRequest) {
    encryptionService.decrypt(awsConfig, encryptionDetails);
    return getAmazonEcsClient(
        region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials())
        .registerTaskDefinition(registerTaskDefinitionRequest);
  }

  public ListServicesResult listServices(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, ListServicesRequest listServicesRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(
          region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials())
          .listServices(listServicesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new ListServicesResult();
  }

  public DescribeServicesResult describeServices(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DescribeServicesRequest describeServicesRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(
          region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials())
          .describeServices(describeServicesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new DescribeServicesResult();
  }

  public CreateServiceResult createService(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, CreateServiceRequest createServiceRequest) {
    encryptionService.decrypt(awsConfig, encryptionDetails);
    return getAmazonEcsClient(
        region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials())
        .createService(createServiceRequest);
  }

  public UpdateServiceResult updateService(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, UpdateServiceRequest updateServiceRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(
          region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials())
          .updateService(updateServiceRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new UpdateServiceResult();
  }

  public DeleteServiceResult deleteService(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DeleteServiceRequest deleteServiceRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(
          region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials())
          .deleteService(deleteServiceRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new DeleteServiceResult();
  }

  public ListTasksResult listTasks(String region, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      ListTasksRequest listTasksRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(
          region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials())
          .listTasks(listTasksRequest);
    } catch (ClusterNotFoundException ex) {
      throw new WingsException(ErrorCode.AWS_CLUSTER_NOT_FOUND).addParam("message", Misc.getMessage(ex));
    } catch (ServiceNotFoundException ex) {
      throw new WingsException(ErrorCode.AWS_SERVICE_NOT_FOUND).addParam("message", Misc.getMessage(ex));
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new ListTasksResult();
  }

  public DescribeTaskDefinitionResult describeTaskDefinition(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DescribeTaskDefinitionRequest describeTaskDefinitionRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(
          region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials())
          .describeTaskDefinition(describeTaskDefinitionRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new DescribeTaskDefinitionResult();
  }

  public DescribeTasksResult describeTasks(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DescribeTasksRequest describeTasksRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(
          region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials())
          .describeTasks(describeTasksRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new DescribeTasksResult();
  }

  public DescribeContainerInstancesResult describeContainerInstances(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails,
      DescribeContainerInstancesRequest describeContainerInstancesRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getAmazonEcsClient(
          region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials())
          .describeContainerInstances(describeContainerInstancesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
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
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new ListImagesResult();
  }

  public ListImagesResult listEcrImages(
      EcrConfig ecrConfig, List<EncryptedDataDetail> encryptedDataDetails, ListImagesRequest listImagesRequest) {
    try {
      return getAmazonEcrClient(ecrConfig, encryptedDataDetails).listImages(listImagesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new ListImagesResult();
  }

  public DescribeRepositoriesResult listRepositories(EcrConfig ecrConfig,
      List<EncryptedDataDetail> encryptedDataDetails, DescribeRepositoriesRequest describeRepositoriesRequest) {
    try {
      return getAmazonEcrClient(ecrConfig, encryptedDataDetails).describeRepositories(describeRepositoriesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
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
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new DescribeRepositoriesResult();
  }

  public List<LoadBalancerDescription> getLoadBalancerDescriptions(
      String region, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      return getClassicElbClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey(),
          awsConfig.isUseEc2IamCredentials())
          .describeLoadBalancers(
              new com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest().withPageSize(400))
          .getLoadBalancerDescriptions();
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  public TargetGroup getTargetGroupForAlb(
      String region, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String targetGroupArn) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient =
          getAmazonElasticLoadBalancingClient(Regions.fromName(region), awsConfig.getAccessKey(),
              awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials());

      DescribeTargetGroupsRequest describeTargetGroupsRequest = new DescribeTargetGroupsRequest().withPageSize(5);
      describeTargetGroupsRequest.withTargetGroupArns(targetGroupArn);

      List<TargetGroup> targetGroupList =
          amazonElasticLoadBalancingClient.describeTargetGroups(describeTargetGroupsRequest).getTargetGroups();
      if (isNotEmpty(targetGroupList)) {
        return targetGroupList.get(0);
      }
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }

    return null;
  }

  public void setAutoScalingGroupCapacityAndWaitForInstancesReadyState(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName, Integer desiredCapacity,
      ManagerExecutionLogCallback executionLogCallback, Integer autoScalingSteadyStateTimeout) {
    encryptionService.decrypt(awsConfig, encryptionDetails);
    AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region),
        awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials());
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
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
  }

  public void setAutoScalingGroupCapacityAndWaitForInstancesReadyState(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName, Integer desiredCapacity,
      ManagerExecutionLogCallback executionLogCallback) {
    setAutoScalingGroupCapacityAndWaitForInstancesReadyState(
        awsConfig, encryptionDetails, region, autoScalingGroupName, desiredCapacity, executionLogCallback, 10);
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
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return null;
  }

  private void waitForAllInstancesToBeReady(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String autoScalingGroupName, Integer desiredCount,
      ManagerExecutionLogCallback executionLogCallback, Integer autoScalingSteadyStateTimeout) {
    try {
      timeLimiter.callWithTimeout(() -> {
        AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region),
            awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials());
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

  public List<Instance> listAutoScalingGroupInstances(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName) {
    List<Instance> instanceList = Lists.newArrayList();
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonEC2Client amazonEc2Client = getAmazonEc2Client(
          region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials());
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
        describeInstancesResult.getReservations().forEach(
            reservation -> instanceList.addAll(reservation.getInstances()));

        describeInstancesRequest.withNextToken(describeInstancesResult.getNextToken());

      } while (describeInstancesResult.getNextToken() != null);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
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

  public List<String> listClassicLoadBalancers(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      List<LoadBalancerDescription> describeLoadBalancers =
          getLoadBalancerDescriptions(region, awsConfig, encryptionDetails);
      return describeLoadBalancers.stream().map(LoadBalancerDescription::getLoadBalancerName).collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  public CreateAutoScalingGroupResult createAutoScalingGroup(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region,
      CreateAutoScalingGroupRequest createAutoScalingGroupRequest, LogCallback logCallback) {
    AmazonAutoScalingClient amazonAutoScalingClient = null;
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region), awsConfig.getAccessKey(),
          awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials());
      return amazonAutoScalingClient.createAutoScalingGroup(createAutoScalingGroupRequest);
    } catch (AmazonServiceException amazonServiceException) {
      if (amazonAutoScalingClient != null && logCallback != null) {
        describeAutoScalingGroupActivities(amazonAutoScalingClient,
            createAutoScalingGroupRequest.getAutoScalingGroupName(), new HashSet<>(), logCallback, true);
      }
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new CreateAutoScalingGroupResult();
  }

  public DescribeAutoScalingGroupsResult describeAutoScalingGroups(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region,
      DescribeAutoScalingGroupsRequest autoScalingGroupsRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region),
          awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials());
      return amazonAutoScalingClient.describeAutoScalingGroups(autoScalingGroupsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new DescribeAutoScalingGroupsResult();
  }

  protected void describeAutoScalingGroupActivities(AmazonAutoScalingClient amazonAutoScalingClient,
      String autoScalingGroupName, Set<String> completedActivities, LogCallback callback, boolean withCause) {
    if (callback == null) {
      logger.info("Not describing autoScalingGroupActivities for {} since logCallback is null", completedActivities);
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
      logger.warn("Failed to describe autoScalingGroup for [{}]", autoScalingGroupName, e);
    }
  }

  public List<Metric> getCloudWatchMetrics(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonCloudWatchClient cloudWatchClient = getAwsCloudWatchClient(
          region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials());

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
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  public List<Metric> getCloudWatchMetrics(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, ListMetricsRequest listMetricsRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonCloudWatchClient cloudWatchClient = getAwsCloudWatchClient(
          region, awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials());

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
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  public boolean registerInstancesWithLoadBalancer(Regions region, String accessKey, char[] secretKey,
      String loadBalancerName, String instanceId, boolean useEc2IamCredentials) {
    try {
      com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient elbClient =
          getClassicElbClient(region, accessKey, secretKey, useEc2IamCredentials);
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
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return false;
  }

  public boolean deregisterInstancesFromLoadBalancer(Regions region, String accessKey, char[] secretKey,
      String loadBalancerName, String instanceId, boolean useEc2IamCredentials) {
    try {
      com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient elbClient =
          getClassicElbClient(region, accessKey, secretKey, useEc2IamCredentials);
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
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return false;
  }

  public CreateStackResult createStack(String region, String accessKey, char[] secretKey,
      CreateStackRequest createStackRequest, boolean useEc2IamCredentials) {
    try {
      AmazonCloudFormationClient cloudFormationClient =
          getAmazonCloudFormationClient(Regions.fromName(region), accessKey, secretKey, useEc2IamCredentials);
      return cloudFormationClient.createStack(createStackRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new CreateStackResult();
  }

  public UpdateStackResult updateStack(String region, String accessKey, char[] secretKey,
      UpdateStackRequest updateStackRequest, boolean useEc2IamCredentials) {
    try {
      AmazonCloudFormationClient cloudFormationClient =
          getAmazonCloudFormationClient(Regions.fromName(region), accessKey, secretKey, useEc2IamCredentials);
      return cloudFormationClient.updateStack(updateStackRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new UpdateStackResult();
  }

  public DescribeStacksResult describeStacks(String region, String accessKey, char[] secretKey,
      DescribeStacksRequest describeStacksRequest, boolean useEc2IamCredentials) {
    try {
      AmazonCloudFormationClient cloudFormationClient =
          getAmazonCloudFormationClient(Regions.fromName(region), accessKey, secretKey, useEc2IamCredentials);
      return cloudFormationClient.describeStacks(describeStacksRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new DescribeStacksResult();
  }

  public List<Stack> getAllStacks(String region, String accessKey, char[] secretKey,
      DescribeStacksRequest describeStacksRequest, boolean useEc2IamCredentials) {
    AmazonCloudFormationClient cloudFormationClient =
        getAmazonCloudFormationClient(Regions.fromName(region), accessKey, secretKey, useEc2IamCredentials);
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
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  public List<StackEvent> getAllStackEvents(String region, String accessKey, char[] secretKey,
      DescribeStackEventsRequest describeStackEventsRequest, boolean useEc2IamCredentials) {
    AmazonCloudFormationClient cloudFormationClient =
        getAmazonCloudFormationClient(Regions.fromName(region), accessKey, secretKey, useEc2IamCredentials);
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
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  public void deleteStack(String region, String accessKey, char[] secretKey, DeleteStackRequest deleteStackRequest,
      boolean useEc2IamCredentials) {
    try {
      AmazonCloudFormationClient cloudFormationClient =
          getAmazonCloudFormationClient(Regions.fromName(region), accessKey, secretKey, useEc2IamCredentials);
      cloudFormationClient.deleteStack(deleteStackRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
  }

  public boolean isVersioningEnabledForBucket(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      BucketVersioningConfiguration bucketVersioningConfiguration =
          getAmazonS3Client(awsConfig.getAccessKey(), awsConfig.getSecretKey(),
              getBucketRegion(awsConfig, encryptionDetails, bucketName), awsConfig.isUseEc2IamCredentials())
              .getBucketVersioningConfiguration(bucketName);
      return "ENABLED".equals(bucketVersioningConfiguration.getStatus());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return false;
  }

  public String getBucketRegion(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      // You can query the bucket location using any region, it returns the result. So, using the default
      String region = getAmazonS3Client(
          awsConfig.getAccessKey(), awsConfig.getSecretKey(), "us-east-1", awsConfig.isUseEc2IamCredentials())
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
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return null;
  }

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
    } catch (IOException e) {
      return false;
    }
  }

  public List<Filter> getAwsFiltersForRunningState() {
    return awsUtils.getAwsFilters(AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping().build());
  }

  public DescribeInstancesRequest getDescribeInstancesRequestWithRunningFilter() {
    return new DescribeInstancesRequest().withFilters(getAwsFiltersForRunningState());
  }
}