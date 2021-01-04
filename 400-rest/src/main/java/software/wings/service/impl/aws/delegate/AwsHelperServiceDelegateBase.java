package software.wings.service.impl.aws.delegate;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.AWS_ACCESS_DENIED;
import static io.harness.eraro.ErrorCode.AWS_CLUSTER_NOT_FOUND;
import static io.harness.eraro.ErrorCode.AWS_SERVICE_NOT_FOUND;
import static io.harness.exception.WingsException.USER;

import static software.wings.service.impl.aws.model.AwsConstants.AWS_DEFAULT_REGION;

import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.aws.AwsCallTracker;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.AwsConfig;
import software.wings.beans.AwsCrossAccountAttributes;
import software.wings.service.intfc.security.EncryptionService;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
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
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class AwsHelperServiceDelegateBase {
  @VisibleForTesting static final String HARNESS_AUTOSCALING_GROUP_TAG = "HARNESS_REVISION";
  @Inject protected EncryptionService encryptionService;
  @Inject protected AwsCallTracker tracker;

  protected void attachCredentials(AwsClientBuilder builder, AwsConfig awsConfig) {
    AWSCredentialsProvider credentialsProvider;

    if (awsConfig.isUseEc2IamCredentials()) {
      log.info("Instantiating EC2ContainerCredentialsProviderWrapper");
      credentialsProvider = new EC2ContainerCredentialsProviderWrapper();
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
