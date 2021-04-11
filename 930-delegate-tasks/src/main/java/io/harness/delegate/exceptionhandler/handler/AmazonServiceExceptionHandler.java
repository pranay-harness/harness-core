package io.harness.delegate.exceptionhandler.handler;

import static io.harness.eraro.ErrorCode.AWS_ACCESS_DENIED;
import static io.harness.eraro.ErrorCode.AWS_CLUSTER_NOT_FOUND;
import static io.harness.eraro.ErrorCode.AWS_SERVICE_NOT_FOUND;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.ExceptionHandler;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.codedeploy.model.AmazonCodeDeployException;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ecs.model.ClusterNotFoundException;
import com.amazonaws.services.ecs.model.ServiceNotFoundException;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
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
    // TODO this is just a sample handler, doesn't cover exhaustive list of AWS exceptions

    AmazonServiceException amazonServiceException = (AmazonServiceException) exception;
    if (amazonServiceException instanceof AmazonCodeDeployException) {
      return new InvalidRequestException(amazonServiceException.getMessage(), AWS_ACCESS_DENIED, USER);
    } else if (amazonServiceException instanceof AmazonEC2Exception) {
      return new InvalidRequestException(amazonServiceException.getMessage(), AWS_ACCESS_DENIED, USER);
    } else if (amazonServiceException instanceof ClusterNotFoundException) {
      return new InvalidRequestException(amazonServiceException.getMessage(), AWS_CLUSTER_NOT_FOUND, USER);
    } else if (amazonServiceException instanceof ServiceNotFoundException) {
      return new InvalidRequestException(amazonServiceException.getMessage(), AWS_SERVICE_NOT_FOUND, USER);
    } else {
      return new InvalidRequestException(amazonServiceException.getMessage(), AWS_SERVICE_NOT_FOUND, USER);
    }
  }
}
