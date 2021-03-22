package io.harness.delegate.exceptionhandler;

import static io.harness.exception.WingsException.ExecutionContext.DELEGATE;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData.ErrorNotifyResponseDataBuilder;
import io.harness.delegate.exceptionhandler.handler.DelegateExceptionHandler;
import io.harness.exception.DelegateErrorHandlerException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.KryoHandlerNotFoundException;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
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

  public DelegateResponseData getResponseData(Throwable throwable,
      ErrorNotifyResponseDataBuilder errorNotifyResponseDataBuilder, boolean isErrorFrameworkSupportedByTask) {
    if (!(throwable instanceof Exception) || !isErrorFrameworkSupportedByTask) {
      // return default response
      return prepareErrorResponse(throwable, errorNotifyResponseDataBuilder).build();
    }

    Exception exception = (Exception) throwable;
    WingsException processedException = handleException(exception);
    DelegateResponseData responseData =
        prepareErrorResponse(processedException, errorNotifyResponseDataBuilder).exception(processedException).build();

    ExceptionLogger.logProcessedMessages(processedException, DELEGATE, log);
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
        if (exception instanceof WingsException) {
          handledException = (WingsException) exception;
        } else {
          throw new DelegateErrorHandlerException("Delegate exception handler not registered");
        }
      }

      if (exception.getCause() != null) {
        WingsException cascadedException = handledException;
        while (cascadedException.getCause() != null) {
          // 3rd party exception can't be allowed as cause in already handled exception
          cascadedException = (WingsException) cascadedException.getCause();
        }
        setExceptionCause(cascadedException, handleException((Exception) exception.getCause()));
      }
      return handledException;

    } catch (Exception e) {
      log.error("Exception occured while handling delegate exception : {}", exception, e);
      return prepareUnhandledExceptionResponse(exception);
    }
  }

  private WingsException prepareUnhandledExceptionResponse(Exception exception) {
    Exception unhandledException = exception;
    if (!kryoSerializer.isRegistered(unhandledException.getClass())) {
      log.error("Kyro handler not found for exception", unhandledException);
      unhandledException = new KryoHandlerNotFoundException(unhandledException.getMessage());
    }
    return new DelegateErrorHandlerException("Unable to handle delegate exception", unhandledException);
  }

  private ErrorNotifyResponseDataBuilder prepareErrorResponse(
      Throwable throwable, ErrorNotifyResponseDataBuilder errorNotifyResponseDataBuilder) {
    if (errorNotifyResponseDataBuilder == null) {
      errorNotifyResponseDataBuilder = ErrorNotifyResponseData.builder();
    }

    return errorNotifyResponseDataBuilder.failureTypes(ExceptionUtils.getFailureTypes(throwable))
        .errorMessage(ExceptionUtils.getMessage(throwable));
  }

  private void setExceptionCause(WingsException exception, Exception cause) throws IllegalAccessException {
    ReflectionUtils.setObjectField(ReflectionUtils.getFieldByName(exception.getClass(), "cause"), exception, cause);
  }
}
