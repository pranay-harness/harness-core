/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.exceptionhandler.handler;

import static io.harness.eraro.ErrorCode.AWS_ACCESS_DENIED;
import static io.harness.eraro.ErrorCode.AWS_CLUSTER_NOT_FOUND;
import static io.harness.eraro.ErrorCode.AWS_SERVICE_NOT_FOUND;
import static io.harness.eraro.ErrorCode.IMAGE_NOT_FOUND;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.context.MdcGlobalContextData;
import io.harness.exception.AwsAutoScaleException;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.IllegalArgumentException;
import io.harness.exception.ImageNotFoundException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.InvalidThirdPartyCredentialsException;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionMetadataKeys;
import io.harness.manage.GlobalContextManager;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.model.AmazonAutoScalingException;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.codedeploy.model.AmazonCodeDeployException;
import com.amazonaws.services.codedeploy.model.InvalidTagException;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ecr.model.AmazonECRException;
import com.amazonaws.services.ecr.model.InvalidParameterException;
import com.amazonaws.services.ecr.model.RepositoryNotFoundException;
import com.amazonaws.services.ecs.model.AmazonECSException;
import com.amazonaws.services.ecs.model.ClusterNotFoundException;
import com.amazonaws.services.ecs.model.ServiceNotFoundException;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.DX)
@Singleton
public class AmazonServiceExceptionHandler implements ExceptionHandler {
  // Create list of exceptions that will be handled by this exception handler
  // and use it while registering to map binder
  public static Set<Class<? extends Exception>> exceptions() {
    return ImmutableSet.<Class<? extends Exception>>builder().add(AmazonServiceException.class).build();
  }

  @Override
  public WingsException handleException(Exception exception) {
    AmazonServiceException ex = (AmazonServiceException) exception;
    if (ex instanceof InvalidTagException) {
      if (GlobalContextManager.get(MdcGlobalContextData.MDC_ID) != null) {
        Map<String, String> imageDetails =
            ((MdcGlobalContextData) GlobalContextManager.get(MdcGlobalContextData.MDC_ID)).getMap();
        return new ExplanationException(String.format(ExplanationException.COMMAND_TRIED_FOR_ARTIFACT,
                                            imageDetails.get(ExceptionMetadataKeys.URL.name())),
            new ExplanationException(String.format(ExplanationException.IMAGE_TAG_METADATA_NOT_FOUND,
                                         imageDetails.get(ExceptionMetadataKeys.IMAGE_NAME.name()),
                                         imageDetails.get(ExceptionMetadataKeys.IMAGE_TAG.name()),
                                         imageDetails.get(ExceptionMetadataKeys.CONNECTOR.name())),
                new HintException(HintException.HINT_INVALID_TAG_REFER_LINK_ECR,
                    new io.harness.exception.InvalidTagException(ex.getMessage(), USER))));
      }
      return new HintException(HintException.HINT_INVALID_TAG_REFER_LINK_ECR,
          new io.harness.exception.InvalidTagException(ex.getMessage(), USER));
    } else if (ex instanceof AmazonCodeDeployException) {
      return new InvalidRequestException(ex.getMessage(), AWS_ACCESS_DENIED, USER);
    } else if (ex instanceof AmazonEC2Exception) {
      return new InvalidRequestException(ex.getMessage(), AWS_ACCESS_DENIED, USER);
    } else if (ex instanceof ClusterNotFoundException) {
      return new InvalidRequestException(ex.getMessage(), AWS_CLUSTER_NOT_FOUND, USER);
    } else if (ex instanceof ServiceNotFoundException) {
      return new InvalidRequestException(ex.getMessage(), AWS_SERVICE_NOT_FOUND, USER);
    } else if (ex instanceof RepositoryNotFoundException) {
      if (GlobalContextManager.get(MdcGlobalContextData.MDC_ID) != null) {
        Map<String, String> imageDetails =
            ((MdcGlobalContextData) GlobalContextManager.get(MdcGlobalContextData.MDC_ID)).getMap();
        return new ExplanationException(String.format(ExplanationException.IMAGE_METADATA_NOT_FOUND,
                                            imageDetails.get(ExceptionMetadataKeys.IMAGE_NAME.name()),
                                            imageDetails.get(ExceptionMetadataKeys.CONNECTOR.name())),
            new HintException(HintException.HINT_INVALID_IMAGE_REFER_LINK_ECR,
                new io.harness.exception.ImageNotFoundException(ex.getMessage(), USER)));
      }
      return new HintException(
          HintException.HINT_ECR_IMAGE_NAME, new ImageNotFoundException(ex.getMessage(), IMAGE_NOT_FOUND, USER));
    } else if (ex instanceof InvalidParameterException) {
      if (GlobalContextManager.get(MdcGlobalContextData.MDC_ID) != null) {
        Map<String, String> imageDetails =
            ((MdcGlobalContextData) GlobalContextManager.get(MdcGlobalContextData.MDC_ID)).getMap();
        return new ExplanationException(String.format(ExplanationException.ILLEGAL_IMAGE_FORMAT,
                                            imageDetails.get(ExceptionMetadataKeys.IMAGE_NAME.name())),
            new HintException(
                HintException.HINT_ILLEGAL_IMAGE_PATH, new IllegalArgumentException(ex.getMessage(), USER)));
      }
      return new ExplanationException("Provided image path does not satisfy ECR image path format",
          new HintException("Please provide valid image path", new IllegalArgumentException(ex.getMessage(), USER)));
    } else if (ex instanceof AmazonECSException || ex instanceof AmazonECRException) {
      if (GlobalContextManager.get(MdcGlobalContextData.MDC_ID) != null) {
        Map<String, String> imageDetails =
            ((MdcGlobalContextData) GlobalContextManager.get(MdcGlobalContextData.MDC_ID)).getMap();
        return new ExplanationException(String.format(ExplanationException.REGISTRY_ACCESS_DENIED,
                                            imageDetails.get(ExceptionMetadataKeys.CONNECTOR.name())),
            new HintException(String.format(HintException.HINT_INVALID_CONNECTOR,
                                  imageDetails.get(ExceptionMetadataKeys.CONNECTOR.name())),
                new InvalidThirdPartyCredentialsException(ex.getMessage(), USER)));
      }
      return new HintException(
          HintException.HINT_AWS_ACCESS_DENIED, new InvalidRequestException(ex.getMessage(), AWS_ACCESS_DENIED, USER));
    } else if (ex instanceof AmazonAutoScalingException) {
      return new AwsAutoScaleException(ex.getMessage(), AWS_SERVICE_NOT_FOUND, USER);
    } else if (ex instanceof AmazonCloudFormationException) {
      if (ex.getMessage().contains("No updates are to be performed")) {
        log.info("Nothing to update on stack" + ex.getMessage());
      }
      return new InvalidRequestException(ex.getMessage(), AWS_SERVICE_NOT_FOUND, USER);
    } else {
      return new InvalidRequestException(ex.getMessage(), AWS_SERVICE_NOT_FOUND, USER);
    }
  }
}
