package io.harness.delegate.exceptionhandler.handler;

import static io.harness.eraro.ErrorCode.AWS_ACCESS_DENIED;
import static io.harness.eraro.ErrorCode.AWS_CLUSTER_NOT_FOUND;
import static io.harness.eraro.ErrorCode.AWS_SERVICE_NOT_FOUND;
import static io.harness.exception.WingsException.USER;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.ExceptionHandler;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.model.AmazonAutoScalingException;
import com.amazonaws.services.cloudformation.model.AmazonCloudFormationException;
import com.amazonaws.services.codedeploy.model.AmazonCodeDeployException;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ecs.model.ClusterNotFoundException;
import com.amazonaws.services.ecs.model.ServiceNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AmazonServiceExceptionHandler implements ExceptionHandler {
  @Override
  public WingsException handleException(Exception exception) {
    AmazonServiceException amazonServiceException = (AmazonServiceException) exception;
    if (amazonServiceException instanceof AmazonCodeDeployException) {
      return new InvalidRequestException(amazonServiceException.getMessage(), AWS_ACCESS_DENIED, USER);
    } else if (amazonServiceException instanceof AmazonEC2Exception) {
      return new InvalidRequestException(amazonServiceException.getMessage(), AWS_ACCESS_DENIED, USER);
    } else if (amazonServiceException instanceof ClusterNotFoundException) {
      return new InvalidRequestException(amazonServiceException.getMessage(), AWS_CLUSTER_NOT_FOUND, USER);
    } else if (amazonServiceException instanceof ServiceNotFoundException) {
      return new InvalidRequestException(amazonServiceException.getMessage(), AWS_SERVICE_NOT_FOUND, USER);
    } else if (amazonServiceException instanceof AmazonAutoScalingException) {
      if (amazonServiceException.getMessage().contains(
              "Trying to remove Target Groups that are not part of the group")) {
        log.info("Target Group already not attached: [{}]", amazonServiceException.getMessage());
      } else if (amazonServiceException.getMessage().contains(
                     "Trying to remove Load Balancers that are not part of the group")) {
        log.info("Classic load balancer already not attached: [{}]", amazonServiceException.getMessage());
      } else {
        log.warn(amazonServiceException.getErrorMessage(), exception);
        //        return exception;
      }
      //    } else if (exception instanceof AmazonECSException
      //            || exception instanceof AmazonECRException) {
      //      if (exception instanceof ClientException) {
      //        log.warn(exception.getErrorMessage(), exception);
      //        throw exception;
      //      }
      throw new InvalidRequestException(amazonServiceException.getMessage(), AWS_ACCESS_DENIED, USER);
    } else if (amazonServiceException instanceof AmazonCloudFormationException) {
      if (amazonServiceException.getMessage().contains("No updates are to be performed")) {
        log.info("Nothing to update on stack" + amazonServiceException.getMessage());
      } else {
        return new InvalidRequestException(amazonServiceException.getMessage(), amazonServiceException, USER);
      }
    } else {
      return new InvalidRequestException(amazonServiceException.getMessage(), amazonServiceException, USER);
    }
    return null;
  }
}
