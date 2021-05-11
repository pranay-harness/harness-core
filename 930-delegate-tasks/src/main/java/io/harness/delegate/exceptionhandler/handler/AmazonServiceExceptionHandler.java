package io.harness.delegate.exceptionhandler.handler;

import static io.harness.eraro.ErrorCode.AWS_ACCESS_DENIED;
import static io.harness.eraro.ErrorCode.AWS_CLUSTER_NOT_FOUND;
import static io.harness.eraro.ErrorCode.AWS_SERVICE_NOT_FOUND;
import static io.harness.eraro.ErrorCode.IMAGE_NOT_FOUND;
import static io.harness.eraro.ErrorCode.IMAGE_TAG_NOT_FOUND;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.context.MdcGlobalContextData;
import io.harness.exception.AwsAutoScaleException;
import io.harness.exception.HintException;
import io.harness.exception.ImageNotFoundException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;
import io.harness.manage.GlobalContextManager;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.model.AmazonAutoScalingException;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.codedeploy.model.AmazonCodeDeployException;
import com.amazonaws.services.codedeploy.model.InvalidTagException;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ecr.model.AmazonECRException;
import com.amazonaws.services.ecr.model.RepositoryNotFoundException;
import com.amazonaws.services.ecs.model.AmazonECSException;
import com.amazonaws.services.ecs.model.ClientException;
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
    AmazonServiceException amazonServiceException = (AmazonServiceException) exception;
    if (amazonServiceException instanceof InvalidTagException) {
      return new ImageNotFoundException(amazonServiceException.getMessage(), IMAGE_TAG_NOT_FOUND, USER);
    } else if (amazonServiceException instanceof AmazonCodeDeployException) {
      return new InvalidRequestException(amazonServiceException.getMessage(), AWS_ACCESS_DENIED, USER);
    } else if (amazonServiceException instanceof AmazonEC2Exception) {
      return new InvalidRequestException(amazonServiceException.getMessage(), AWS_ACCESS_DENIED, USER);
    } else if (amazonServiceException instanceof ClusterNotFoundException) {
      return new InvalidRequestException(amazonServiceException.getMessage(), AWS_CLUSTER_NOT_FOUND, USER);
    } else if (amazonServiceException instanceof ServiceNotFoundException) {
      return new InvalidRequestException(amazonServiceException.getMessage(), AWS_SERVICE_NOT_FOUND, USER);
    } else if (amazonServiceException instanceof RepositoryNotFoundException) {
      if (GlobalContextManager.get(MdcGlobalContextData.MDC_ID) != null) {
        Map<String, String> imageDetails =
            ((MdcGlobalContextData) GlobalContextManager.get(MdcGlobalContextData.MDC_ID)).getMap();
        return new HintException("ECR image: '" + imageDetails.get("imageName") + "' not found in region: '"
                + imageDetails.get("region") + "'",
            new ImageNotFoundException(amazonServiceException.getMessage(), IMAGE_NOT_FOUND, USER));
      }
      return new HintException(HintException.HINT_ECR_IMAGE_NAME,
          new ImageNotFoundException(amazonServiceException.getMessage(), IMAGE_NOT_FOUND, USER));
    } else if (amazonServiceException instanceof AmazonECSException
        || amazonServiceException instanceof AmazonECRException) {
      if (amazonServiceException instanceof ClientException) {
        log.warn(amazonServiceException.getErrorMessage(), amazonServiceException);
      }
      return new HintException(HintException.HINT_AWS_ACCESS_DENIED,
          new InvalidRequestException(amazonServiceException.getMessage(), AWS_ACCESS_DENIED, USER));
    } else if (amazonServiceException instanceof AmazonAutoScalingException) {
      return new AwsAutoScaleException(amazonServiceException.getMessage(), AWS_SERVICE_NOT_FOUND, USER);
    } else if (amazonServiceException instanceof AmazonCloudFormationException) {
      if (amazonServiceException.getMessage().contains("No updates are to be performed")) {
        log.info("Nothing to update on stack" + amazonServiceException.getMessage());
      }
      return new InvalidRequestException(amazonServiceException.getMessage(), AWS_SERVICE_NOT_FOUND, USER);
    } else {
      return new InvalidRequestException(amazonServiceException.getMessage(), AWS_SERVICE_NOT_FOUND, USER);
    }
  }
}
