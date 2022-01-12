/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.aws;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.AWS_ACCESS_DENIED;
import static io.harness.eraro.ErrorCode.AWS_CLUSTER_NOT_FOUND;
import static io.harness.eraro.ErrorCode.AWS_SERVICE_NOT_FOUND;
import static io.harness.exception.WingsException.USER;

import io.harness.exception.InvalidRequestException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.model.AmazonAutoScalingException;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.codedeploy.model.AmazonCodeDeployException;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ecr.model.AmazonECRException;
import com.amazonaws.services.ecs.model.AmazonECSException;
import com.amazonaws.services.ecs.model.ClientException;
import com.amazonaws.services.ecs.model.ClusterNotFoundException;
import com.amazonaws.services.ecs.model.ServiceNotFoundException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class AwsExceptionHandler {
  public static void handleAmazonServiceException(AmazonServiceException amazonServiceException) {
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

  public static void handleAmazonClientException(AmazonClientException amazonClientException) {
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
}
