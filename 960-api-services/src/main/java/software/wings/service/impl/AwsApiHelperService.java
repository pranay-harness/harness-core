package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;

import static software.wings.service.impl.aws.model.AwsConstants.AWS_DEFAULT_REGION;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_BACKOFF_MAX_ERROR_RETRIES;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.aws.AwsCallTracker;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.eraro.ErrorCode;
import io.harness.exception.AwsAutoScaleException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.serializer.JsonUtils;

import software.wings.beans.AwsCrossAccountAttributes;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.retry.PredefinedBackoffStrategies;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.autoscaling.model.AmazonAutoScalingException;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.codedeploy.model.AmazonCodeDeployException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.Region;
import com.amazonaws.services.ecr.AmazonECRClient;
import com.amazonaws.services.ecr.AmazonECRClientBuilder;
import com.amazonaws.services.ecr.model.AmazonECRException;
import com.amazonaws.services.ecr.model.BatchGetImageRequest;
import com.amazonaws.services.ecr.model.DescribeRepositoriesRequest;
import com.amazonaws.services.ecr.model.DescribeRepositoriesResult;
import com.amazonaws.services.ecr.model.Image;
import com.amazonaws.services.ecr.model.ImageIdentifier;
import com.amazonaws.services.ecr.model.ListImagesRequest;
import com.amazonaws.services.ecr.model.ListImagesResult;
import com.amazonaws.services.ecs.model.AmazonECSException;
import com.amazonaws.services.ecs.model.ClientException;
import com.amazonaws.services.ecs.model.ClusterNotFoundException;
import com.amazonaws.services.ecs.model.ServiceNotFoundException;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class AwsApiHelperService {
  @Inject private AwsCallTracker tracker;

  public AmazonECRClient getAmazonEcrClient(AwsInternalConfig awsConfig, String region) {
    AmazonECRClientBuilder builder = AmazonECRClientBuilder.standard().withRegion(region);
    attachCredentialsAndBackoffPolicy(builder, awsConfig);
    return (AmazonECRClient) builder.build();
  }
  public AmazonEC2Client getAmazonEc2Client(AwsInternalConfig awsConfig) {
    AmazonEC2ClientBuilder builder = AmazonEC2ClientBuilder.standard().withRegion(getRegion(awsConfig));
    attachCredentialsAndBackoffPolicy(builder, awsConfig);
    return (AmazonEC2Client) builder.build();
  }

  public List<String> listRegions(AwsInternalConfig awsConfig) {
    try {
      AmazonEC2Client amazonEC2Client = getAmazonEc2Client(awsConfig);
      tracker.trackEC2Call("List Regions");
      return amazonEC2Client.describeRegions().getRegions().stream().map(Region::getRegionName).collect(toList());
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }
  public ListImagesResult listEcrImages(
      AwsInternalConfig awsConfig, String region, ListImagesRequest listImagesRequest) {
    return getAmazonEcrClient(awsConfig, region).listImages(listImagesRequest);
  }
  public DescribeRepositoriesResult listRepositories(
      AwsInternalConfig awsConfig, DescribeRepositoriesRequest describeRepositoriesRequest, String region) {
    try {
      tracker.trackECRCall("List Repositories");
      return getAmazonEcrClient(awsConfig, region).describeRepositories(describeRepositoriesRequest);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return new DescribeRepositoriesResult();
  }

  public Map<String, String> fetchLabels(
      AwsInternalConfig awsConfig, String imageName, String region, List<String> tags) {
    AmazonECRClient ecrClient = getAmazonEcrClient(awsConfig, region);
    return tags.stream()
        .map(tag
            -> ecrClient.batchGetImage(
                new BatchGetImageRequest()
                    .withRepositoryName(imageName)
                    .withImageIds(new ImageIdentifier().withImageTag(tag))
                    .withAcceptedMediaTypes("application/vnd.docker.distribution.manifest.v1+json")))
        .flatMap(batchGetImageResult -> batchGetImageResult.getImages().stream())
        .map(Image::getImageManifest)
        .flatMap(imageManifest
            -> ((List<Map<String, Object>>) JsonUtils.asObject(imageManifest, HashMap.class).get("history"))
                   .stream()
                   .flatMap(history
                       -> ((Map<String, Object>) (JsonUtils.asObject(
                               (String) history.get("v1Compatibility"), HashMap.class)))
                              .entrySet()
                              .stream()))
        .filter(entry -> entry.getKey().equals("config"))
        .flatMap(config
            -> ((Map<String, String>) ((Map<String, Object>) config.getValue()).get("Labels")).entrySet().stream())
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public void attachCredentialsAndBackoffPolicy(AwsClientBuilder builder, AwsInternalConfig awsConfig) {
    AWSCredentialsProvider credentialsProvider;
    if (awsConfig.isUseEc2IamCredentials()) {
      log.info("Instantiating EC2ContainerCredentialsProviderWrapper");
      credentialsProvider = new EC2ContainerCredentialsProviderWrapper();
    } else {
      credentialsProvider = new AWSStaticCredentialsProvider(
          new BasicAWSCredentials(new String(awsConfig.getAccessKey()), new String(awsConfig.getSecretKey())));
    }
    if (awsConfig.isAssumeCrossAccountRole()) {
      // For the security token service we default to us-east-1.
      AWSSecurityTokenService securityTokenService =
          AWSSecurityTokenServiceClientBuilder.standard()
              .withRegion(isNotBlank(awsConfig.getDefaultRegion()) ? awsConfig.getDefaultRegion() : AWS_DEFAULT_REGION)
              .withCredentials(credentialsProvider)
              .build();
      AwsCrossAccountAttributes crossAccountAttributes = awsConfig.getCrossAccountAttributes();
      credentialsProvider = new STSAssumeRoleSessionCredentialsProvider
                                .Builder(crossAccountAttributes.getCrossAccountRoleArn(), UUID.randomUUID().toString())
                                .withStsClient(securityTokenService)
                                .withExternalId(crossAccountAttributes.getExternalId())
                                .build();
    }

    builder.withCredentials(credentialsProvider);
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    RetryPolicy retryPolicy = new RetryPolicy(new PredefinedRetryPolicies.SDKDefaultRetryCondition(),
        new PredefinedBackoffStrategies.SDKDefaultBackoffStrategy(), DEFAULT_BACKOFF_MAX_ERROR_RETRIES, false);
    clientConfiguration.setRetryPolicy(retryPolicy);
    builder.withClientConfiguration(clientConfiguration);
  }

  public void handleAmazonClientException(AmazonClientException amazonClientException) {
    log.error("AWS API Client call exception", amazonClientException);
    String errorMessage = amazonClientException.getMessage();
    if (isNotEmpty(errorMessage) && errorMessage.contains("/meta-data/iam/security-credentials/")) {
      throw new InvalidRequestException("The IAM role on the Ec2 delegate does not exist OR does not"
              + " have required permissions.",
          amazonClientException, USER);
    } else {
      log.error("Unhandled aws exception");
      throw new InvalidRequestException(
          amazonClientException.getMessage() != null ? amazonClientException.getMessage() : "Exception Message",
          ErrorCode.AWS_ACCESS_DENIED, USER);
    }
  }

  public void handleAmazonServiceException(AmazonServiceException amazonServiceException) {
    log.error("AWS API call exception", amazonServiceException);
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
      throw new AwsAutoScaleException(amazonServiceException.getMessage(), ErrorCode.GENERAL_ERROR, USER);
    } else if (amazonServiceException instanceof AmazonECSException
        || amazonServiceException instanceof AmazonECRException) {
      if (amazonServiceException instanceof ClientException) {
        log.warn(amazonServiceException.getErrorMessage(), amazonServiceException);
        throw amazonServiceException;
      }
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED).addParam("message", amazonServiceException.getMessage());
    } else if (amazonServiceException instanceof AmazonCloudFormationException) {
      if (amazonServiceException.getMessage().contains("No updates are to be performed")) {
        log.info("Nothing to update on stack" + amazonServiceException.getMessage());
      } else {
        throw new InvalidRequestException(amazonServiceException.getMessage(), amazonServiceException);
      }
    } else {
      log.error("Unhandled aws exception");
      throw new WingsException(ErrorCode.AWS_ACCESS_DENIED).addParam("message", amazonServiceException.getMessage());
    }
  }

  private String getRegion(AwsInternalConfig awsConfig) {
    if (isNotBlank(awsConfig.getDefaultRegion())) {
      return awsConfig.getDefaultRegion();
    } else {
      return AWS_DEFAULT_REGION;
    }
  }
}
