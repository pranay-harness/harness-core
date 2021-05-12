package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INIT_TIMEOUT;
import static io.harness.eraro.ErrorCode.INVALID_CLOUD_PROVIDER;
import static io.harness.exception.WingsException.USER;
import static io.harness.threading.Morpheus.sleep;

import static software.wings.service.impl.aws.model.AwsConstants.AWS_DEFAULT_REGION;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.aws.AwsCallTracker;
import io.harness.concurrent.HTimeLimiter;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.network.Http;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.annotation.EncryptableSetting;
import software.wings.beans.AWSTemporaryCredentials;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.EcrConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.common.InfrastructureConstants;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.helpers.ext.amazons3.AWSTemporaryCredentialsRestClient;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.mappers.artifact.AwsConfigToInternalMapper;
import software.wings.sm.states.ManagerExecutionLogCallback;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.applicationautoscaling.AWSApplicationAutoScaling;
import com.amazonaws.services.applicationautoscaling.AWSApplicationAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.Activity;
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
import com.amazonaws.services.ec2.model.ResourceType;
import com.amazonaws.services.ec2.model.TagDescription;
import com.amazonaws.services.ecr.AmazonECRClient;
import com.amazonaws.services.ecr.AmazonECRClientBuilder;
import com.amazonaws.services.ecr.model.DescribeRepositoriesRequest;
import com.amazonaws.services.ecr.model.DescribeRepositoriesResult;
import com.amazonaws.services.ecr.model.GetAuthorizationTokenRequest;
import com.amazonaws.services.ecr.model.ListImagesRequest;
import com.amazonaws.services.ecr.model.ListImagesResult;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.ClusterNotFoundException;
import com.amazonaws.services.ecs.model.CreateClusterRequest;
import com.amazonaws.services.ecs.model.CreateClusterResult;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.CreateServiceResult;
import com.amazonaws.services.ecs.model.DeleteServiceRequest;
import com.amazonaws.services.ecs.model.DeleteServiceResult;
import com.amazonaws.services.ecs.model.DeregisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.DeregisterTaskDefinitionResult;
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
import com.amazonaws.services.ecs.model.RunTaskRequest;
import com.amazonaws.services.ecs.model.RunTaskResult;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.ServiceField;
import com.amazonaws.services.ecs.model.ServiceNotFoundException;
import com.amazonaws.services.ecs.model.TagResourceRequest;
import com.amazonaws.services.ecs.model.TagResourceResult;
import com.amazonaws.services.ecs.model.UntagResourceRequest;
import com.amazonaws.services.ecs.model.UntagResourceResult;
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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import org.apache.commons.collections.CollectionUtils;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Singleton
@Slf4j
@TargetModule(HarnessModule._960_API_SERVICES)
@OwnedBy(CDP)
public class AwsHelperService {
  @Inject private EncryptionService encryptionService;
  @Inject private TimeLimiter timeLimiter;
  @Inject private ManagerExpressionEvaluator expressionEvaluator;
  @Inject private AwsUtils awsUtils;
  @Inject private AwsCallTracker tracker;
  @Inject private AwsApiHelperService awsApiHelperService;

  private static final long AUTOSCALING_REQUEST_STATUS_CHECK_INTERVAL = TimeUnit.SECONDS.toSeconds(15);
  public AmazonECRClient getAmazonEcrClient(AwsConfig awsConfig, String region) {
    return awsApiHelperService.getAmazonEcrClient(AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig), region);
  }
  public AmazonEC2Client getAmazonEc2Client(AwsConfig awsConfig) {
    return awsApiHelperService.getAmazonEc2Client(AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig));
  }

  public boolean validateAwsAccountCredential(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackEC2Call("Describe Regions");
      getAmazonEc2Client(awsConfig).describeRegions();
    } catch (AmazonEC2Exception amazonEC2Exception) {
      if (amazonEC2Exception.getStatusCode() == 401) {
        throw new InvalidRequestException("Invalid AWS credentials", INVALID_CLOUD_PROVIDER, USER);
      }
    }
    return true;
  }

  public void validateAwsAccountCredential(String accessKey, char[] secretKey) {
    try {
      getAmazonEc2Client(AwsConfig.builder().accessKey(accessKey.toCharArray()).secretKey(secretKey).build())
          .describeRegions();
      tracker.trackEC2Call("Describe Regions");
    } catch (AmazonEC2Exception amazonEC2Exception) {
      if (amazonEC2Exception.getStatusCode() == 401) {
        throw new InvalidRequestException("Invalid AWS credentials", ErrorCode.INVALID_CLOUD_PROVIDER, USER);
      }
    }
  }

  public AmazonCloudWatchClient getAwsCloudWatchClient(String region, AwsConfig awsConfig) {
    AmazonCloudWatchClientBuilder builder = AmazonCloudWatchClientBuilder.standard().withRegion(region);
    awsApiHelperService.attachCredentialsAndBackoffPolicy(
        builder, AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig));
    return (AmazonCloudWatchClient) builder.build();
  }

  private AmazonECSClient getAmazonEcsClient(String region, AwsConfig awsConfig) {
    AmazonECSClientBuilder builder = AmazonECSClientBuilder.standard().withRegion(region);
    awsApiHelperService.attachCredentialsAndBackoffPolicy(
        builder, AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig));
    return (AmazonECSClient) builder.build();
  }

  private AWSApplicationAutoScaling getAWSApplicationAutoScalingClient(
      String region, String accessKey, char[] secretKey) {
    return AWSApplicationAutoScalingClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
  }

  public AmazonECRClient getAmazonEcrClient(EcrConfig ecrConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    encryptionService.decrypt(ecrConfig, encryptedDataDetails, false);
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
    encryptionService.decrypt(ecrConfig, encryptedDataDetails, false);
    AmazonECRClient ecrClient = (AmazonECRClient) AmazonECRClientBuilder.standard()
                                    .withRegion(ecrConfig.getRegion())
                                    .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(
                                        ecrConfig.getAccessKey(), new String(ecrConfig.getSecretKey()))))
                                    .build();

    String url = ecrConfig.getEcrUrl();
    // Example: https://830767422336.dkr.ecr.us-east-1.amazonaws.com/
    String awsAccount = url.substring(8, url.indexOf('.'));
    tracker.trackECRCall("Get Auth Token");
    return ecrClient
        .getAuthorizationToken(new GetAuthorizationTokenRequest().withRegistryIds(singletonList(awsAccount)))
        .getAuthorizationData()
        .get(0)
        .getAuthorizationToken();
  }

  private AmazonS3Client getAmazonS3Client(String region, AwsConfig awsConfig) {
    AmazonS3ClientBuilder builder =
        AmazonS3ClientBuilder.standard().withRegion(region).withForceGlobalBucketAccessEnabled(true);
    awsApiHelperService.attachCredentialsAndBackoffPolicy(
        builder, AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig));
    return (AmazonS3Client) builder.build();
  }

  private AmazonS3Client getAmazonS3Client(AwsConfig awsConfig) {
    AmazonS3ClientBuilder builder =
        AmazonS3ClientBuilder.standard().withRegion(getRegion(awsConfig)).withForceGlobalBucketAccessEnabled(true);
    awsApiHelperService.attachCredentialsAndBackoffPolicy(
        builder, AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig));
    return (AmazonS3Client) builder.build();
  }

  private AmazonEC2Client getAmazonEc2Client(String region, AwsConfig awsConfig) {
    AmazonEC2ClientBuilder builder = AmazonEC2ClientBuilder.standard().withRegion(region);
    awsApiHelperService.attachCredentialsAndBackoffPolicy(
        builder, AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig));
    return (AmazonEC2Client) builder.build();
  }

  private AmazonCodeDeployClient getAmazonCodeDeployClient(Regions region, AwsConfig awsConfig) {
    AmazonCodeDeployClientBuilder builder = AmazonCodeDeployClientBuilder.standard().withRegion(region);
    awsApiHelperService.attachCredentialsAndBackoffPolicy(
        builder, AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig));
    return (AmazonCodeDeployClient) builder.build();
  }

  @VisibleForTesting
  AmazonCloudFormationClient getAmazonCloudFormationClient(Regions region, AwsConfig awsConfig) {
    AmazonCloudFormationClientBuilder builder = AmazonCloudFormationClientBuilder.standard().withRegion(region);
    awsApiHelperService.attachCredentialsAndBackoffPolicy(
        builder, AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig));
    return (AmazonCloudFormationClient) builder.build();
  }

  @VisibleForTesting
  AmazonAutoScalingClient getAmazonAutoScalingClient(Regions region, AwsConfig awsConfig) {
    AmazonAutoScalingClientBuilder builder = AmazonAutoScalingClientBuilder.standard().withRegion(region);
    awsApiHelperService.attachCredentialsAndBackoffPolicy(
        builder, AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig));
    return (AmazonAutoScalingClient) builder.build();
  }

  private AmazonElasticLoadBalancingClient getAmazonElasticLoadBalancingClient(Regions region, AwsConfig awsConfig) {
    com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder builder =
        com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder.standard().withRegion(
            region);
    awsApiHelperService.attachCredentialsAndBackoffPolicy(
        builder, AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig));
    return (AmazonElasticLoadBalancingClient) builder.build();
  }

  private com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient getClassicElbClient(
      Regions region, AwsConfig awsConfig) {
    AmazonElasticLoadBalancingClientBuilder builder =
        AmazonElasticLoadBalancingClientBuilder.standard().withRegion(region);
    awsApiHelperService.attachCredentialsAndBackoffPolicy(
        builder, AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig));
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
      hostNameConvention = InfrastructureConstants.DEFAULT_AWS_HOST_NAME_CONVENTION;
    }
    return expressionEvaluator.substitute(hostNameConvention, context);
  }

  public String getInstanceId(Regions region, String hostName, AwsConfig awsConfig) {
    AmazonEC2Client amazonEC2Client = getAmazonEc2Client(region.getName(), awsConfig);

    String instanceId;
    tracker.trackEC2Call("Describe Instances");
    DescribeInstancesResult describeInstancesResult = amazonEC2Client.describeInstances(
        new DescribeInstancesRequest().withFilters(new Filter("private-dns-name").withValues(hostName + "*")));
    instanceId = describeInstancesResult.getReservations()
                     .stream()
                     .flatMap(reservation -> reservation.getInstances().stream())
                     .map(Instance::getInstanceId)
                     .findFirst()
                     .orElse(null);

    if (isBlank(instanceId)) {
      tracker.trackEC2Call("Describe Instances");
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
      tracker.trackEC2Call("Describe Instances");
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
      tracker.trackEC2Call("Describe Instances");
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
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackS3Call("List Buckets");
      return getAmazonS3Client(awsConfig).listBuckets();
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return Collections.emptyList();
  }

  public S3Object getObjectFromS3(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName, String key) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackS3Call("Get Object");
      return getAmazonS3Client(getBucketRegion(awsConfig, encryptionDetails, bucketName), awsConfig)
          .getObject(bucketName, key);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return null;
  }

  public ObjectMetadata getObjectMetadataFromS3(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName, String key) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackS3Call("Get Object Metadata");
      return getAmazonS3Client(getBucketRegion(awsConfig, encryptionDetails, bucketName), awsConfig)
          .getObjectMetadata(bucketName, key);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return null;
  }

  public ListObjectsV2Result listObjectsInS3(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, ListObjectsV2Request listObjectsV2Request) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackS3Call("Get Bucket Region");
      AmazonS3Client amazonS3Client = getAmazonS3Client(
          getBucketRegion(awsConfig, encryptionDetails, listObjectsV2Request.getBucketName()), awsConfig);
      return amazonS3Client.listObjectsV2(listObjectsV2Request);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new ListObjectsV2Result();
  }

  public AwsConfig validateAndGetAwsConfig(
      SettingAttribute connectorConfig, List<EncryptedDataDetail> encryptedDataDetails, boolean isInstanceSync) {
    if (connectorConfig == null || connectorConfig.getValue() == null
        || !(connectorConfig.getValue() instanceof AwsConfig)) {
      throw new InvalidRequestException("connectorConfig is not of type AwsConfig");
    }
    encryptionService.decrypt((EncryptableSetting) connectorConfig.getValue(), encryptedDataDetails, isInstanceSync);
    return (AwsConfig) connectorConfig.getValue();
  }

  public ListDeploymentGroupsResult listDeploymentGroupsResult(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region,
      ListDeploymentGroupsRequest listDeploymentGroupsRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackCDCall("List Deployment Groups");
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig)
          .listDeploymentGroups(listDeploymentGroupsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new ListDeploymentGroupsResult();
  }

  public ListApplicationsResult listApplicationsResult(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, ListApplicationsRequest listApplicationsRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackCDCall("List Applications");
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig).listApplications(listApplicationsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new ListApplicationsResult();
  }

  public ListDeploymentConfigsResult listDeploymentConfigsResult(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region,
      ListDeploymentConfigsRequest listDeploymentConfigsRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackCDCall("List Deployment Configs");
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig)
          .listDeploymentConfigs(listDeploymentConfigsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new ListDeploymentConfigsResult();
  }

  public GetDeploymentResult getCodeDeployDeployment(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, GetDeploymentRequest getDeploymentRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackCDCall("Get Deployment");
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig).getDeployment(getDeploymentRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new GetDeploymentResult();
  }

  public GetDeploymentGroupResult getCodeDeployDeploymentGroup(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region, GetDeploymentGroupRequest getDeploymentGroupRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackCDCall("Get Deployment Group");
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig)
          .getDeploymentGroup(getDeploymentGroupRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new GetDeploymentGroupResult();
  }

  public CreateDeploymentResult createCodeDeployDeployment(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region, CreateDeploymentRequest createDeploymentRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackCDCall("Create Deployment");
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig).createDeployment(createDeploymentRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new CreateDeploymentResult();
  }

  public ListDeploymentInstancesResult listDeploymentInstances(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region,
      ListDeploymentInstancesRequest listDeploymentInstancesRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackCDCall("List Deployment Instances");
      return getAmazonCodeDeployClient(Regions.fromName(region), awsConfig)
          .listDeploymentInstances(listDeploymentInstancesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
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
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackEC2Call("Describe Instances");
      return getAmazonEc2Client(region, awsConfig).describeInstances(describeInstancesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new DescribeInstancesResult();
  }

  public DescribeImagesResult desribeEc2Images(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, DescribeImagesRequest describeImagesRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      AmazonEC2Client amazonEc2Client = getAmazonEc2Client(region, awsConfig);
      tracker.trackEC2Call("Describe Images");
      return amazonEc2Client.describeImages(describeImagesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new DescribeImagesResult();
  }

  public List<String> listRegions(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    encryptionService.decrypt(awsConfig, encryptionDetails, false);
    return awsApiHelperService.listRegions(AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig));
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
        encryptionService.decrypt(awsConfig, encryptionDetails, false);
        AmazonEC2Client amazonEC2Client = getAmazonEc2Client(region, awsConfig);
        tracker.trackEC2Call("Describe Tags");

        DescribeTagsResult describeTagsResult = amazonEC2Client.describeTags(
            new DescribeTagsRequest()
                .withNextToken(nextToken)
                .withFilters(new Filter("resource-type").withValues(resourceType.toString()))
                .withMaxResults(1000));
        tags.addAll(describeTagsResult.getTags().stream().map(TagDescription::getKey).collect(Collectors.toSet()));
        nextToken = describeTagsResult.getNextToken();
      } while (nextToken != null);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return tags;
  }

  public CreateClusterResult createCluster(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, CreateClusterRequest createClusterRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackECSCall("Create Cluster");
      return getAmazonEcsClient(region, awsConfig).createCluster(createClusterRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new CreateClusterResult();
  }

  public DescribeClustersResult describeClusters(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DescribeClustersRequest describeClustersRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackECSCall("Describe Cluster");
      return getAmazonEcsClient(region, awsConfig).describeClusters(describeClustersRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new DescribeClustersResult();
  }

  public ListClustersResult listClusters(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, ListClustersRequest listClustersRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackECSCall("List Clusters");
      return getAmazonEcsClient(region, awsConfig).listClusters(listClustersRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new ListClustersResult();
  }

  public RegisterTaskDefinitionResult registerTaskDefinition(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, RegisterTaskDefinitionRequest registerTaskDefinitionRequest) {
    encryptionService.decrypt(awsConfig, encryptionDetails, false);
    tracker.trackECSCall("Register Task Definition");
    return getAmazonEcsClient(region, awsConfig).registerTaskDefinition(registerTaskDefinitionRequest);
  }

  public ListServicesResult listServices(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, ListServicesRequest listServicesRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackECSCall("List Services");
      return getAmazonEcsClient(region, awsConfig).listServices(listServicesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new ListServicesResult();
  }

  public DescribeServicesResult describeServices(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DescribeServicesRequest describeServicesRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackECSCall("Describe Services");
      return getAmazonEcsClient(region, awsConfig).describeServices(describeServicesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new DescribeServicesResult();
  }

  public List<Service> getServiceForCluster(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String cluster, String region) {
    ListServicesRequest listServicesRequest = new ListServicesRequest().withCluster(cluster);
    ListServicesResult listServicesResult = listServices(region, awsConfig, encryptionDetails, listServicesRequest);

    List<Service> services = new ArrayList<>();

    if (isNotEmpty(listServicesResult.getServiceArns())) {
      do {
        services.addAll(describeServices(region, awsConfig, encryptionDetails,
            new DescribeServicesRequest()
                .withCluster(cluster)
                .withServices(listServicesResult.getServiceArns())
                .withInclude(ServiceField.TAGS))
                            .getServices());

        listServicesRequest.setNextToken(listServicesResult.getNextToken());
      } while (listServicesResult.getNextToken() != null && listServicesResult.getServiceArns().size() == 10);
    }

    return services;
  }

  public CreateServiceResult createService(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, CreateServiceRequest createServiceRequest) {
    encryptionService.decrypt(awsConfig, encryptionDetails, false);
    tracker.trackECSCall("Create Service");
    return getAmazonEcsClient(region, awsConfig).createService(createServiceRequest);
  }

  public RunTaskResult triggerEcsRunTask(
      String region, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, RunTaskRequest runTaskRequest) {
    encryptionService.decrypt(awsConfig, encryptionDetails, false);
    tracker.trackECSCall("Ecs Run Task Request");
    return getAmazonEcsClient(region, awsConfig).runTask(runTaskRequest);
  }

  public UpdateServiceResult updateService(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, UpdateServiceRequest updateServiceRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackECSCall("Update Service");
      return getAmazonEcsClient(region, awsConfig).updateService(updateServiceRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new UpdateServiceResult();
  }

  public DeleteServiceResult deleteService(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DeleteServiceRequest deleteServiceRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackECSCall("Delete Service");
      return getAmazonEcsClient(region, awsConfig).deleteService(deleteServiceRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new DeleteServiceResult();
  }

  public DeregisterTaskDefinitionResult deregisterTaskDefinitions(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DeregisterTaskDefinitionRequest deregisterTaskDefinitionRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackECSCall("List Tasks");
      return getAmazonEcsClient(region, awsConfig).deregisterTaskDefinition(deregisterTaskDefinitionRequest);
    } catch (ClusterNotFoundException ex) {
      throw new WingsException(ErrorCode.AWS_CLUSTER_NOT_FOUND).addParam("message", ExceptionUtils.getMessage(ex));
    } catch (ServiceNotFoundException ex) {
      throw new WingsException(ErrorCode.AWS_SERVICE_NOT_FOUND).addParam("message", ExceptionUtils.getMessage(ex));
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new DeregisterTaskDefinitionResult();
  }

  public ListTasksResult listTasks(String region, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      ListTasksRequest listTasksRequest, boolean isInstanceSync) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, isInstanceSync);
      tracker.trackECSCall("List Tasks");
      return getAmazonEcsClient(region, awsConfig).listTasks(listTasksRequest.withMaxResults(100));
    } catch (ClusterNotFoundException ex) {
      throw new WingsException(ErrorCode.AWS_CLUSTER_NOT_FOUND).addParam("message", ExceptionUtils.getMessage(ex));
    } catch (ServiceNotFoundException ex) {
      throw new WingsException(ErrorCode.AWS_SERVICE_NOT_FOUND).addParam("message", ExceptionUtils.getMessage(ex));
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new ListTasksResult();
  }

  public DescribeTaskDefinitionResult describeTaskDefinition(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DescribeTaskDefinitionRequest describeTaskDefinitionRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackECSCall("Desscribe Task Definition");
      return getAmazonEcsClient(region, awsConfig).describeTaskDefinition(describeTaskDefinitionRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new DescribeTaskDefinitionResult();
  }

  public DescribeTasksResult describeTasks(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DescribeTasksRequest describeTasksRequest, boolean isInstanceSync) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, isInstanceSync);
      tracker.trackECSCall("Describe Tasks");
      return getAmazonEcsClient(region, awsConfig).describeTasks(describeTasksRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new DescribeTasksResult();
  }

  public DescribeContainerInstancesResult describeContainerInstances(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails,
      DescribeContainerInstancesRequest describeContainerInstancesRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackECSCall("Describe Container Instances");
      return getAmazonEcsClient(region, awsConfig).describeContainerInstances(describeContainerInstancesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new DescribeContainerInstancesResult();
  }

  public ListImagesResult listEcrImages(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      ListImagesRequest listImagesRequest) {
    encryptionService.decrypt(awsConfig, encryptionDetails, false);
    return awsApiHelperService.listEcrImages(
        AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig), region, listImagesRequest);
  }

  public ListImagesResult listEcrImages(
      EcrConfig ecrConfig, List<EncryptedDataDetail> encryptedDataDetails, ListImagesRequest listImagesRequest) {
    try {
      tracker.trackECRCall("List Images");
      return getAmazonEcrClient(ecrConfig, encryptedDataDetails).listImages(listImagesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new ListImagesResult();
  }

  public DescribeRepositoriesResult listRepositories(EcrConfig ecrConfig,
      List<EncryptedDataDetail> encryptedDataDetails, DescribeRepositoriesRequest describeRepositoriesRequest) {
    try {
      tracker.trackECRCall("List Repositories");
      return getAmazonEcrClient(ecrConfig, encryptedDataDetails).describeRepositories(describeRepositoriesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new DescribeRepositoriesResult();
  }

  public DescribeRepositoriesResult listRepositories(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      DescribeRepositoriesRequest describeRepositoriesRequest, String region) {
    encryptionService.decrypt(awsConfig, encryptionDetails, false);
    return awsApiHelperService.listRepositories(
        AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig), describeRepositoriesRequest, region);
  }

  public List<LoadBalancerDescription> getLoadBalancerDescriptions(
      String region, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    try {
      tracker.trackClassicELBCall("Describe Load Balancers");
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      return getClassicElbClient(Regions.fromName(region), awsConfig)
          .describeLoadBalancers(
              new com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest().withPageSize(400))
          .getLoadBalancerDescriptions();
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  public TargetGroup getTargetGroupForAlb(
      String region, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String targetGroupArn) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      AmazonElasticLoadBalancingClient amazonElasticLoadBalancingClient =
          getAmazonElasticLoadBalancingClient(Regions.fromName(region), awsConfig);
      DescribeTargetGroupsRequest describeTargetGroupsRequest = new DescribeTargetGroupsRequest().withPageSize(5);
      describeTargetGroupsRequest.withTargetGroupArns(targetGroupArn);
      tracker.trackELBCall("Describe Target Groups");
      List<TargetGroup> targetGroupList =
          amazonElasticLoadBalancingClient.describeTargetGroups(describeTargetGroupsRequest).getTargetGroups();
      if (isNotEmpty(targetGroupList)) {
        if (targetGroupList.get(0).getTargetGroupArn().equalsIgnoreCase(targetGroupArn)) {
          return targetGroupList.get(0);
        }
      }
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return null;
  }

  public void setAutoScalingGroupCapacityAndWaitForInstancesReadyState(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName, Integer desiredCapacity,
      ManagerExecutionLogCallback executionLogCallback, Integer autoScalingSteadyStateTimeout) {
    encryptionService.decrypt(awsConfig, encryptionDetails, false);
    AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);
    try {
      executionLogCallback.saveExecutionLog(
          format("Set AutoScaling Group: [%s] desired capacity to [%s]", autoScalingGroupName, desiredCapacity));
      tracker.trackASGCall("Set ASG Desired Capacity");
      amazonAutoScalingClient.setDesiredCapacity(new SetDesiredCapacityRequest()
                                                     .withAutoScalingGroupName(autoScalingGroupName)
                                                     .withDesiredCapacity(desiredCapacity));
      executionLogCallback.saveExecutionLog("Successfully set desired capacity");
      waitForAllInstancesToBeReady(awsConfig, encryptionDetails, region, autoScalingGroupName, desiredCapacity,
          executionLogCallback, autoScalingSteadyStateTimeout);
    } catch (AmazonServiceException amazonServiceException) {
      describeAutoScalingGroupActivities(
          amazonAutoScalingClient, autoScalingGroupName, new HashSet<>(), executionLogCallback, true);
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
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
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return null;
  }

  private void waitForAllInstancesToBeReady(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String autoScalingGroupName, Integer desiredCount,
      ManagerExecutionLogCallback executionLogCallback, Integer autoScalingSteadyStateTimeout) {
    try {
      HTimeLimiter.callInterruptible(timeLimiter, Duration.ofMinutes(autoScalingSteadyStateTimeout), () -> {
        AmazonAutoScalingClient amazonAutoScalingClient =
            getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);
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
      });
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
    List<Instance> instanceList = newArrayList();
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      AmazonEC2Client amazonEc2Client = getAmazonEc2Client(region, awsConfig);
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
        tracker.trackEC2Call("Describe Instances.");
        describeInstancesResult = amazonEc2Client.describeInstances(describeInstancesRequest);
        describeInstancesResult.getReservations().forEach(
            reservation -> instanceList.addAll(reservation.getInstances()));

        describeInstancesRequest.withNextToken(describeInstancesResult.getNextToken());

      } while (describeInstancesResult.getNextToken() != null);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return instanceList;
  }

  public List<String> listInstanceIdsFromAutoScalingGroup(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String autoScalingGroupName) {
    AutoScalingGroup group = getAutoScalingGroup(awsConfig, encryptionDetails, region, autoScalingGroupName);
    if (group != null) {
      return group.getInstances()
          .stream()
          .map(com.amazonaws.services.autoscaling.model.Instance::getInstanceId)
          .collect(toList());
    } else {
      return emptyList();
    }
  }

  public List<String> listClassicLoadBalancers(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      List<LoadBalancerDescription> describeLoadBalancers =
          getLoadBalancerDescriptions(region, awsConfig, encryptionDetails);
      return describeLoadBalancers.stream().map(LoadBalancerDescription::getLoadBalancerName).collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  public CreateAutoScalingGroupResult createAutoScalingGroup(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region,
      CreateAutoScalingGroupRequest createAutoScalingGroupRequest, LogCallback logCallback) {
    AmazonAutoScalingClient amazonAutoScalingClient = null;
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);
      tracker.trackASGCall("Create ASG");
      return amazonAutoScalingClient.createAutoScalingGroup(createAutoScalingGroupRequest);
    } catch (AmazonServiceException amazonServiceException) {
      if (amazonAutoScalingClient != null && logCallback != null) {
        describeAutoScalingGroupActivities(amazonAutoScalingClient,
            createAutoScalingGroupRequest.getAutoScalingGroupName(), new HashSet<>(), logCallback, true);
      }
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new CreateAutoScalingGroupResult();
  }

  public DescribeAutoScalingGroupsResult describeAutoScalingGroups(AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, String region,
      DescribeAutoScalingGroupsRequest autoScalingGroupsRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackASGCall("Describe ASGs");
      AmazonAutoScalingClient amazonAutoScalingClient = getAmazonAutoScalingClient(Regions.fromName(region), awsConfig);
      return amazonAutoScalingClient.describeAutoScalingGroups(autoScalingGroupsRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new DescribeAutoScalingGroupsResult();
  }

  protected void describeAutoScalingGroupActivities(AmazonAutoScalingClient amazonAutoScalingClient,
      String autoScalingGroupName, Set<String> completedActivities, LogCallback callback, boolean withCause) {
    if (callback == null) {
      log.info("Not describing autoScalingGroupActivities for {} since logCallback is null", completedActivities);
      return;
    }
    try {
      tracker.trackASGCall("Describe Scaling Activities");
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
      log.warn("Failed to describe autoScalingGroup for [{}]", autoScalingGroupName, e);
    }
  }

  public List<Metric> getCloudWatchMetrics(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      AmazonCloudWatchClient cloudWatchClient = getAwsCloudWatchClient(region, awsConfig);
      List<Metric> rv = new ArrayList<>();
      String nextToken = null;
      do {
        ListMetricsRequest request = new ListMetricsRequest();
        request.withNextToken(nextToken);
        tracker.trackCloudWatchCall("List Metrics");
        ListMetricsResult listMetricsResult = cloudWatchClient.listMetrics(request);
        nextToken = listMetricsResult.getNextToken();
        rv.addAll(listMetricsResult.getMetrics());
      } while (nextToken != null);

      return rv;
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  public List<Metric> getCloudWatchMetrics(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, ListMetricsRequest listMetricsRequest) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      AmazonCloudWatchClient cloudWatchClient = getAwsCloudWatchClient(region, awsConfig);

      List<Metric> rv = new ArrayList<>();
      String nextToken = null;
      do {
        listMetricsRequest.withNextToken(nextToken);
        tracker.trackCloudWatchCall("List Metrics");
        ListMetricsResult listMetricsResult = cloudWatchClient.listMetrics(listMetricsRequest);
        nextToken = listMetricsResult.getNextToken();
        rv.addAll(listMetricsResult.getMetrics());
      } while (nextToken != null);

      return rv;

    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  public boolean registerInstancesWithLoadBalancer(
      Regions region, String loadBalancerName, String instanceId, AwsConfig awsConfig) {
    try {
      com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient elbClient =
          getClassicElbClient(region, awsConfig);
      tracker.trackClassicELBCall("Register Instances");
      return elbClient
          .registerInstancesWithLoadBalancer(
              new RegisterInstancesWithLoadBalancerRequest()
                  .withLoadBalancerName(loadBalancerName)
                  .withInstances(new com.amazonaws.services.elasticloadbalancing.model.Instance(instanceId)))
          .getInstances()
          .stream()
          .anyMatch(inst -> inst.getInstanceId().equals(instanceId));
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return false;
  }

  public boolean deregisterInstancesFromLoadBalancer(
      Regions region, String loadBalancerName, String instanceId, AwsConfig awsConfig) {
    try {
      com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient elbClient =
          getClassicElbClient(region, awsConfig);
      tracker.trackClassicELBCall("De register Instances");
      return elbClient
          .deregisterInstancesFromLoadBalancer(
              new DeregisterInstancesFromLoadBalancerRequest()
                  .withLoadBalancerName(loadBalancerName)
                  .withInstances(new com.amazonaws.services.elasticloadbalancing.model.Instance(instanceId)))
          .getInstances()
          .stream()
          .noneMatch(inst -> inst.getInstanceId().equals(instanceId));
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return false;
  }

  public CreateStackResult createStack(String region, CreateStackRequest createStackRequest, AwsConfig awsConfig) {
    try {
      AmazonCloudFormationClient cloudFormationClient =
          getAmazonCloudFormationClient(Regions.fromName(region), awsConfig);
      tracker.trackCFCall("Create Stack");
      return cloudFormationClient.createStack(createStackRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new CreateStackResult();
  }

  public UpdateStackResult updateStack(String region, UpdateStackRequest updateStackRequest, AwsConfig awsConfig) {
    try {
      AmazonCloudFormationClient cloudFormationClient =
          getAmazonCloudFormationClient(Regions.fromName(region), awsConfig);
      tracker.trackCFCall("Update Stack");
      return cloudFormationClient.updateStack(updateStackRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new UpdateStackResult();
  }

  public DescribeStacksResult describeStacks(
      String region, DescribeStacksRequest describeStacksRequest, AwsConfig awsConfig) {
    try {
      AmazonCloudFormationClient cloudFormationClient =
          getAmazonCloudFormationClient(Regions.fromName(region), awsConfig);
      tracker.trackCFCall("Describe Stacks");
      return cloudFormationClient.describeStacks(describeStacksRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return new DescribeStacksResult();
  }

  public List<Stack> getAllStacks(String region, DescribeStacksRequest describeStacksRequest, AwsConfig awsConfig) {
    AmazonCloudFormationClient cloudFormationClient =
        getAmazonCloudFormationClient(Regions.fromName(region), awsConfig);
    try {
      List<Stack> stacks = new ArrayList<>();
      String nextToken = null;
      do {
        describeStacksRequest.withNextToken(nextToken);
        tracker.trackCFCall("Describe Stacks");
        DescribeStacksResult result = cloudFormationClient.describeStacks(describeStacksRequest);
        nextToken = result.getNextToken();
        stacks.addAll(result.getStacks());
      } while (nextToken != null);
      return stacks;
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  public List<StackEvent> getAllStackEvents(
      String region, DescribeStackEventsRequest describeStackEventsRequest, AwsConfig awsConfig) {
    AmazonCloudFormationClient cloudFormationClient =
        getAmazonCloudFormationClient(Regions.fromName(region), awsConfig);
    try {
      List<StackEvent> stacksEvents = new ArrayList<>();
      String nextToken = null;
      do {
        describeStackEventsRequest.withNextToken(nextToken);
        tracker.trackCFCall("Describe Stack Events");
        DescribeStackEventsResult result = cloudFormationClient.describeStackEvents(describeStackEventsRequest);
        nextToken = result.getNextToken();
        stacksEvents.addAll(result.getStackEvents());
      } while (nextToken != null);
      return stacksEvents;
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  public void deleteStack(String region, DeleteStackRequest deleteStackRequest, AwsConfig awsConfig) {
    try {
      AmazonCloudFormationClient cloudFormationClient =
          getAmazonCloudFormationClient(Regions.fromName(region), awsConfig);
      tracker.trackCFCall("Delete Stack");
      cloudFormationClient.deleteStack(deleteStackRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
  }

  public boolean isVersioningEnabledForBucket(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      tracker.trackS3Call("Get Bucket Versioning Configuration");
      BucketVersioningConfiguration bucketVersioningConfiguration =
          getAmazonS3Client(getBucketRegion(awsConfig, encryptionDetails, bucketName), awsConfig)
              .getBucketVersioningConfiguration(bucketName);
      return "ENABLED".equals(bucketVersioningConfiguration.getStatus());
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return false;
  }

  public String getBucketRegion(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails, false);
      // You can query the bucket location using any region, it returns the result. So, using the default
      String region = getAmazonS3Client(awsConfig).getBucketLocation(bucketName);
      // Aws returns US if the bucket was created in the default region. Not sure why it doesn't return just the region
      // name in all cases. Also, their documentation says it would return empty string if its in the default region.
      // http://docs.aws.amazon.com/AmazonS3/latest/API/RESTBucketGETlocation.html But it returns US. Added additional
      // checks based on other stuff
      if (region == null || region.equals("US")) {
        return AWS_DEFAULT_REGION;
      } else if (region.equals("EU")) {
        return "eu-west-1";
      }
      return region;
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    }
    return null;
  }

  public static boolean isInAwsRegion(String region) {
    // Regions.getCurrentRegion() internally call Ec2Metadata api.
    // It also does exception handling and returns null in that case.
    // This will work except when delegate is run as fargate task.
    com.amazonaws.regions.Region regionForContainer = Regions.getCurrentRegion();

    if (regionForContainer != null) {
      return regionForContainer.getName().equals(region);
    }

    // When delegate is running as fargate task, rely on ENV variable: AWS_REGION
    String currentRegion = System.getenv("AWS_REGION");
    log.info("ECS Current Region Value from ENV var {AWS_REGION}: " + currentRegion);
    if (isNotBlank(currentRegion)) {
      return currentRegion.equals(region);
    }

    log.info("Failed in ECS validation, failed to fetch current region");
    return false;
  }

  public List<Filter> getAwsFiltersForRunningState() {
    return awsUtils.getAwsFilters(AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping().build(), null);
  }

  public DescribeInstancesRequest getDescribeInstancesRequestWithRunningFilter() {
    return new DescribeInstancesRequest().withFilters(getAwsFiltersForRunningState());
  }

  public TagResourceResult tagService(String region, List<EncryptedDataDetail> encryptedDataDetails,
      TagResourceRequest tagResourceRequest, AwsConfig awsConfig) {
    encryptionService.decrypt(awsConfig, encryptedDataDetails, false);
    tracker.trackECSCall("Tag Resource");
    return getAmazonEcsClient(region, awsConfig).tagResource(tagResourceRequest);
  }

  public UntagResourceResult untagService(String region, List<EncryptedDataDetail> encryptedDataDetails,
      UntagResourceRequest untagResourceRequest, AwsConfig awsConfig) {
    encryptionService.decrypt(awsConfig, encryptedDataDetails, false);
    tracker.trackECSCall("Untag Resource");
    return getAmazonEcsClient(region, awsConfig).untagResource(untagResourceRequest);
  }

  AWSTemporaryCredentialsRestClient getAWSTemporaryCredentialsRestClient(String url, AwsConfig awsConfig) {
    OkHttpClient okHttpClient = Http.getOkHttpClient(url, awsConfig.isCertValidationRequired());
    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(url)
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build();
    return retrofit.create(AWSTemporaryCredentialsRestClient.class);
  }

  @VisibleForTesting
  public AWSTemporaryCredentials getCredentialsForIAMROleOnDelegate(String url, AwsConfig awsConfig) {
    AWSTemporaryCredentialsRestClient credentialsRestClient = getAWSTemporaryCredentialsRestClient(url, awsConfig);
    String roleName;
    try {
      Response<ResponseBody> response = credentialsRestClient.getRoleName().execute();
      roleName = response.body().string();
    } catch (IOException e) {
      throw new InvalidRequestException("Cannot get the role name", e);
    }

    if (isEmpty(roleName)) {
      throw new InvalidRequestException("No role attached to the instance");
    }
    try {
      Response<AWSTemporaryCredentials> response = credentialsRestClient.getTemporaryCredentials(roleName).execute();
      if (response.isSuccessful()) {
        return response.body();
      } else {
        throw new InvalidRequestException(
            "Cannot get the temporary credentials, api returned error code: " + response.code());
      }

    } catch (IOException e) {
      throw new InvalidRequestException("Cannot get the temporary credentials", e);
    }
  }

  public Map<String, String> fetchLabels(
      AwsConfig awsConfig, ArtifactStreamAttributes artifactStreamAttributes, List<String> tags) {
    return awsApiHelperService.fetchLabels(AwsConfigToInternalMapper.toAwsInternalConfig(awsConfig),
        artifactStreamAttributes.getImageName(), artifactStreamAttributes.getRegion(), tags);
  }

  private String getRegion(AwsConfig awsConfig) {
    if (isNotBlank(awsConfig.getDefaultRegion())) {
      return awsConfig.getDefaultRegion();
    } else {
      return AWS_DEFAULT_REGION;
    }
  }
}
