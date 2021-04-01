package io.harness.delegate.exceptionhandler.handler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.ExceptionHandler;

import com.amazonaws.AmazonClientException;

@OwnedBy(HarnessTeam.DX)
public class AmazonClientExceptionHandler implements ExceptionHandler {
  @Override
  public WingsException handleException(Exception exception) {
    // TODO this is just a sample handler, doesn't cover exhaustive list of AWS exceptions

    AmazonClientException amazonClientException = (AmazonClientException) exception;
    //        log.error("AWS API Client call exception: {}", amazonClientException.getMessage());
    String errorMessage = amazonClientException.getMessage();
    if (isNotEmpty(errorMessage) && errorMessage.contains("/meta-data/iam/security-credentials/")) {
      return NestedExceptionUtils.hintWithExplanationException(HintException.HINT_AWS_IAM_ROLE_CHECK,
          ExplanationException.EXPLANATION_AWS_AM_ROLE_CHECK,
          new InvalidRequestException("The IAM role on the Ec2 delegate does not exist OR does not"
                  + " have required permissions.",
              amazonClientException, USER));
    } else {
      //            log.error("Unhandled aws exception");
      return NestedExceptionUtils.hintWithExplanationException(HintException.HINT_AWS_CLIENT_UNKNOWN_ISSUE,
          ExplanationException.EXPLANATION_AWS_AM_ROLE_CHECK,
          new InvalidRequestException(isNotEmpty(errorMessage) ? errorMessage : "Unknown Aws client exception", USER));
    }
  }
}
