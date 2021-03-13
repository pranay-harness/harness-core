package io.harness.delegate.exceptionhandler;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.exceptionhandler.handler.DelegateExceptionHandler;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class DelegateExceptionManager {
  private Map<Class<? extends Exception>, DelegateExceptionHandler> exceptionHandler;

  @Inject
  public DelegateExceptionManager(Map<Class<? extends Exception>, DelegateExceptionHandler> exceptionHandlerMapping) {
    exceptionHandler = exceptionHandlerMapping;
  }

  public DelegateResponseData getResponseData(Exception exception,
      ErrorNotifyResponseData.ErrorNotifyResponseDataBuilder errorNotifyResponseDataBuilder,
      boolean isErrorFramwworkSupportedByTask) {
    if (!isErrorFramwworkSupportedByTask) {
      // return default response
      return prepareErrorResponse(exception, errorNotifyResponseDataBuilder).build();
    }

    WingsException exception1 = handleException(exception);
    DelegateResponseData responseData =
        prepareErrorResponse(exception1, errorNotifyResponseDataBuilder).exception(exception1).build();

    return responseData;
  }

  // ---------- PRIVATE METHODS -------------

  private DelegateExceptionHandler getExceptionHandler(Exception exception) {
    return exceptionHandler.entrySet()
        .stream()
        .filter(e -> e.getKey().isInstance(exception))
        .map(Map.Entry::getValue)
        .findFirst()
        .orElse(null);
  }

  private WingsException handleException(Exception exception) {
    DelegateExceptionHandler delegateExceptionHandler = getExceptionHandler(exception);
    try {
      if (delegateExceptionHandler != null) {
        return delegateExceptionHandler.handleException(exception);
      }
    } catch (Exception e) {
      log.error("Exception occured while handling delegate exception : {}", exception, e);
    }

    return prepareDefaultErrorResponse(exception);
  }

  private WingsException prepareDefaultErrorResponse(Exception exception) {
    // TODO create new exception type - UnhandledException and use it here
    return new WingsException(exception);
  }

  private ErrorNotifyResponseData.ErrorNotifyResponseDataBuilder prepareErrorResponse(
      Exception exception, ErrorNotifyResponseData.ErrorNotifyResponseDataBuilder errorNotifyResponseDataBuilder) {
    if (errorNotifyResponseDataBuilder == null) {
      errorNotifyResponseDataBuilder = ErrorNotifyResponseData.builder();
    }

    return errorNotifyResponseDataBuilder.failureTypes(ExceptionUtils.getFailureTypes(exception))
        .errorMessage(ExceptionUtils.getMessage(exception));
  }
}
