package io.harness.delegate.exceptionhandler;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.exceptionhandler.handler.DelegateExceptionHandler;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;

import com.google.inject.Singleton;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class DelegateExceptionManager {
  //  private static final ConcurrentMap<Class<? extends Exception>, DelegateExceptionHandler> CurrGenExceptionHandlers
  //  =
  //      new ConcurrentHashMap<>();
  private static final ConcurrentMap<Class<? extends Exception>, DelegateExceptionHandler> NGExceptionHandlers =
      new ConcurrentHashMap<>();

  public static void registerHandler(Class<? extends Exception> exceptionClass,
      DelegateExceptionHandler delegateExceptionHandler, DelegateExceptionHandlerType exceptionHandlerType) {
    if (delegateExceptionHandler != null && exceptionClass != null) {
      if (DelegateExceptionHandlerType.NG.equals(exceptionHandlerType)) {
        NGExceptionHandlers.putIfAbsent(exceptionClass, delegateExceptionHandler);
      } else {
        CurrGenExceptionHandlers.putIfAbsent(exceptionClass, delegateExceptionHandler);
      }
    } else {
      log.error("Invalid attempt to register delegate exception handler for exception : {} and delegate handler : {}",
          exceptionClass, delegateExceptionHandler);
    }
  }

  public static DelegateResponseData getResponseData(
      Exception exception, ErrorNotifyResponseData.ErrorNotifyResponseDataBuilder errorNotifyResponseDataBuilder) {
    if (errorNotifyResponseDataBuilder == null) {
      errorNotifyResponseDataBuilder = ErrorNotifyResponseData.builder();
    }

    WingsException exception1 = handleException(exception);
    DelegateResponseData responseData =
        errorNotifyResponseDataBuilder.failureTypes(ExceptionUtils.getFailureTypes(exception1))
            .errorMessage(ExceptionUtils.getMessage(exception1))
            .build();

    if (isNGExceptionHandler(exception1)) {
      // Use exception field only in case of error handling as per latest framework
      errorNotifyResponseDataBuilder.exception(exception1);
    }

    return responseData;
  }

  // ---------- PRIVATE METHODS -------------

  private static WingsException handleException(Exception exception) {
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

  private static DelegateExceptionHandler getExceptionHandler(Exception exception) {
    DelegateExceptionHandler exceptionHandler = CurrGenExceptionHandlers.getOrDefault(exception.getClass(), null);
    if (exceptionHandler == null) {
      exceptionHandler = NGExceptionHandlers.getOrDefault(exception.getClass(), null);
    }
    return exceptionHandler;
  }

  private static WingsException prepareDefaultErrorResponse(Exception exception) {
    // TODO create new exception type - UnhandledException and use it here
    return new WingsException(exception);
  }

  private static boolean isNGExceptionHandler(Exception exception) {
    return NGExceptionHandlers.containsKey(exception.getClass());
  }
}
