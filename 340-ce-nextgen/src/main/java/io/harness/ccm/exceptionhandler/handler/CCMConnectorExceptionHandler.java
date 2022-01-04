package io.harness.ccm.exceptionhandler.handler;

import static io.harness.eraro.ErrorCode.CONNECTOR_VALIDATION_EXCEPTION;
import static io.harness.exception.WingsException.USER;

import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;
import io.harness.exception.runtime.CCMConnectorRuntimeException;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CCMConnectorExceptionHandler implements ExceptionHandler {
  // Create list of exceptions that will be handled by this exception handler
  // and use it while registering to map binder
  public static Set<Class<? extends Exception>> exceptions() {
    return ImmutableSet.<Class<? extends Exception>>builder().add(CCMConnectorRuntimeException.class).build();
  }

  @Override
  public WingsException handleException(Exception exception) {
    CCMConnectorRuntimeException ex = (CCMConnectorRuntimeException) exception;
    log.info("Exception message: {}", ex.getMessage());
    if (ex.getCode().equals(CONNECTOR_VALIDATION_EXCEPTION)) {
      return NestedExceptionUtils.hintWithExplanationException(HintException.HINT_INVALID_GIT_REPO,
          ExplanationException.INVALID_GIT_REPO, new InvalidRequestException(ex.getMessage(), USER));
    }
    return NestedExceptionUtils.hintWithExplanationException(HintException.HINT_INVALID_GIT_REPO,
        ExplanationException.INVALID_GIT_REPO, new InvalidRequestException(ex.getMessage(), USER));
  }
}
