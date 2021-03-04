package software.wings.service.impl.aws.delegate;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.*;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.retry.PredefinedBackoffStrategies;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.autoscaling.model.AmazonAutoScalingException;
import com.amazonaws.services.autoscaling.model.TagDescription;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.codedeploy.model.AmazonCodeDeployException;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ecr.model.AmazonECRException;
import com.amazonaws.services.ecs.model.AmazonECSException;
import com.amazonaws.services.ecs.model.ClientException;
import com.amazonaws.services.ecs.model.ClusterNotFoundException;
import com.amazonaws.services.ecs.model.ServiceNotFoundException;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.aws.AwsCallTracker;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsCrossAccountAttributes;
import software.wings.service.intfc.security.EncryptionService;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.*;
import static io.harness.exception.WingsException.USER;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.service.impl.aws.model.AwsConstants.AWS_DEFAULT_REGION;
import static software.wings.service.impl.aws.model.AwsConstants.DEFAULT_BACKOFF_MAX_ERROR_RETRIES;

@Slf4j
@TargetModule(Module._930_DELEGATE_TASKS)
class AwsHelperServiceDelegateBase {
  @VisibleForTesting static final String HARNESS_AUTOSCALING_GROUP_TAG = "HARNESS_REVISION";
  @Inject protected EncryptionService encryptionService;
  @Inject protected AwsCallTracker tracker;

  protected void attachCredentialsAndBackoffPolicy(AwsClientBuilder builder, AwsConfig awsConfig) {
    AWSCredentialsProvider credentialsProvider;

    if (awsConfig.isUseEc2IamCredentials()) {
      log.info("Instantiating EC2ContainerCredentialsProviderWrapper");
      credentialsProvider = new EC2ContainerCredentialsProviderWrapper();
    } else if (awsConfig.isUseIRSA()) {
      WebIdentityTokenCredentialsProvider.Builder providerBuilder = WebIdentityTokenCredentialsProvider.builder();
      providerBuilder.roleSessionName(awsConfig.getAccountId()
          + md5Hex(
              awsConfig.getAccountId() + String.valueOf(ThreadLocalRandom.current().nextDouble()).getBytes(UTF_8)));

      credentialsProvider = providerBuilder.build();
    } else {
      credentialsProvider = new AWSStaticCredentialsProvider(
          new BasicAWSCredentials(defaultString(String.valueOf(awsConfig.getAccessKey()), ""),
              awsConfig.getSecretKey() != null ? new String(awsConfig.getSecretKey()) : ""));
    }
    if (awsConfig.isAssumeCrossAccountRole() && awsConfig.getCrossAccountAttributes() != null) {
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

  @VisibleForTesting
  void handleAmazonClientException(AmazonClientException amazonClientException) {
    log.error("AWS API Client call exception: {}", amazonClientException.getMessage());
    String errorMessage = amazonClientException.getMessage();
    if (isNotEmpty(errorMessage) && errorMessage.contains("/meta-data/iam/security-credentials/")) {
      throw new InvalidRequestException("The IAM role on the Ec2 delegate does not exist OR does not"
              + " have required permissions.",
          amazonClientException, USER);
    } else {
      log.error("Unhandled aws exception");
      throw new InvalidRequestException(isNotEmpty(errorMessage) ? errorMessage : "Unknown Aws client exception", USER);
    }
  }

  @VisibleForTesting
  void handleAmazonServiceException(AmazonServiceException amazonServiceException) {
    log.error("AWS API call exception: {}", amazonServiceException.getMessage());
    if (amazonServiceException instanceof AmazonCodeDeployException) {
      throw new InvalidRequestException(amazonServiceException.getMessage(), AWS_ACCESS_DENIED, USER);
    } else if (amazonServiceException instanceof AmazonEC2Exception) {
      throw new InvalidRequestException(amazonServiceException.getMessage(), AWS_ACCESS_DENIED, USER);
    } else if (amazonServiceException instanceof ClusterNotFoundException) {
      throw new InvalidRequestException(amazonServiceException.getMessage(), AWS_CLUSTER_NOT_FOUND, USER);
    } else if (amazonServiceException instanceof ServiceNotFoundException) {
      throw new InvalidRequestException(amazonServiceException.getMessage(), AWS_SERVICE_NOT_FOUND, USER);
    } else if (amazonServiceException instanceof AmazonAutoScalingException) {
      if (amazonServiceException.getMessage().contains(
              "Trying to remove Target Groups that are not part of the group")) {
        log.info("Target Group already not attached: [{}]", amazonServiceException.getMessage());
      } else if (amazonServiceException.getMessage().contains(
                     "Trying to remove Load Balancers that are not part of the group")) {
        log.info("Classic load balancer already not attached: [{}]", amazonServiceException.getMessage());
      } else {
        log.warn(amazonServiceException.getErrorMessage(), amazonServiceException);
        throw amazonServiceException;
      }
    } else if (amazonServiceException instanceof AmazonECSException
        || amazonServiceException instanceof AmazonECRException) {
      if (amazonServiceException instanceof ClientException) {
        log.warn(amazonServiceException.getErrorMessage(), amazonServiceException);
        throw amazonServiceException;
      }
      throw new InvalidRequestException(amazonServiceException.getMessage(), AWS_ACCESS_DENIED, USER);
    } else if (amazonServiceException instanceof AmazonCloudFormationException) {
      if (amazonServiceException.getMessage().contains("No updates are to be performed")) {
        log.info("Nothing to update on stack" + amazonServiceException.getMessage());
      } else {
        throw new InvalidRequestException(amazonServiceException.getMessage(), amazonServiceException, USER);
      }
    } else {
      throw new InvalidRequestException(amazonServiceException.getMessage(), amazonServiceException, USER);
    }
  }

  protected boolean isHarnessManagedTag(String infraMappingId, TagDescription tagDescription) {
    return tagDescription.getKey().equals(HARNESS_AUTOSCALING_GROUP_TAG)
        && tagDescription.getValue().startsWith(infraMappingId);
  }

  protected String getRegion(AwsConfig awsConfig) {
    if (isNotBlank(awsConfig.getDefaultRegion())) {
      return awsConfig.getDefaultRegion();
    } else {
      return AWS_DEFAULT_REGION;
    }
  }
}
