package io.harness.delegate.exceptionhandler;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData.ErrorNotifyResponseDataBuilder;
import io.harness.delegate.exceptionhandler.handler.DelegateExceptionHandler;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.UnexpectedException;
import io.harness.exception.WingsException;
import io.harness.reflection.ReflectionUtils;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class DelegateExceptionManager {
  @Inject private Map<Class<? extends Exception>, DelegateExceptionHandler> exceptionHandler;
  @Inject private KryoSerializer kryoSerializer;

  //  @Inject
  //  public DelegateExceptionManager(Map<Class<? extends Exception>, DelegateExceptionHandler> exceptionHandlerMapping,
  //  ) {
  //    exceptionHandler = exceptionHandlerMapping;
  //
  //  }

  public DelegateResponseData getResponseData(Exception exception,
      ErrorNotifyResponseDataBuilder errorNotifyResponseDataBuilder, boolean isErrorFrameworkSupportedByTask) {
    if (!isErrorFrameworkSupportedByTask) {
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
    WingsException handledException;
    DelegateExceptionHandler delegateExceptionHandler = getExceptionHandler(exception);
    try {
      if (delegateExceptionHandler != null) {
        handledException = delegateExceptionHandler.handleException(exception);
      } else {
        // check if exception is wingsException, throw custom exception
        // log it here and return default response
        handledException = (WingsException) exception;
      }

      if (exception.getCause() != null) {
        setExceptionCause(handledException, handleException((Exception) exception.getCause()));
      }
      return handledException;

    } catch (Exception e) {
      log.error("Exception occured while handling delegate exception : {}", exception, e);
    }

    return prepareDefaultErrorResponse(exception);
  }

  private WingsException prepareDefaultErrorResponse(Exception exception) {
    if (!kryoSerializer.isRegistered(exception.getClass())) {
      // log -- unable to handle exception due to non-rgistration to kryo
      // define new custom exception --- kryo wrapper
      // set message from exception
    }
    return new UnexpectedException("Unable to handle delegate exception", exception);
  }

  private ErrorNotifyResponseDataBuilder prepareErrorResponse(
      Exception exception, ErrorNotifyResponseDataBuilder errorNotifyResponseDataBuilder) {
    if (errorNotifyResponseDataBuilder == null) {
      errorNotifyResponseDataBuilder = ErrorNotifyResponseData.builder();
    }

    return errorNotifyResponseDataBuilder.failureTypes(ExceptionUtils.getFailureTypes(exception))
        .errorMessage(ExceptionUtils.getMessage(exception));
  }

  private void setExceptionCause(WingsException exception, Exception cause) throws IllegalAccessException {
    ReflectionUtils.setObjectField(ReflectionUtils.getFieldByName(exception.getClass(), "cause"), exception, cause);
  }
}
