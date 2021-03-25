package io.harness.delegate.exceptionhandler;

import static io.harness.exception.WingsException.ExecutionContext.DELEGATE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
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
@OwnedBy(HarnessTeam.DX)
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
    WingsException kryoSerializableException = ensureExceptionIsKryoSerializable(processedException);
    DelegateResponseData responseData = prepareErrorResponse(processedException, errorNotifyResponseDataBuilder)
                                            .exception(kryoSerializableException)
                                            .build();

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
    try {
      WingsException handledException;
      if (exception instanceof WingsException) {
        handledException = (WingsException) exception;
        if (handledException.getCause() != null) {
          setExceptionCause(handledException, handleException((Exception) handledException.getCause()));
        }
      } else {
        DelegateExceptionHandler delegateExceptionHandler = getExceptionHandler(exception);
        if (delegateExceptionHandler != null) {
          handledException = delegateExceptionHandler.handleException(exception);
        } else {
          throw new DelegateErrorHandlerException(
              "Delegate exception handler not registered for exception : " + exception);
        }
        if (exception.getCause() != null) {
          WingsException cascadedException = handledException;
          while (cascadedException.getCause() != null) {
            // 3rd party exception can't be allowed as cause in already handled exception
            cascadedException = (WingsException) cascadedException.getCause();
          }
          setExceptionCause(cascadedException, handleException((Exception) exception.getCause()));
        }
      }
      return handledException;
    } catch (Exception e) {
      log.error("Exception occured while handling delegate exception : {}", exception, e);
      return prepareUnhandledExceptionResponse(exception);
    }
  }

  private WingsException prepareUnhandledExceptionResponse(Exception exception) {
    // default is to wrap unknown exception into wings exception using its message
    return new DelegateErrorHandlerException(exception.getMessage());
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

  private boolean isExceptionKryoRegistered(WingsException wingsException) {
    return kryoSerializer.isRegistered(wingsException.getClass());
  }

  private WingsException handleExceptionIfNotKryoRegistered(WingsException wingsException) {
    if (!isExceptionKryoRegistered(wingsException)) {
      log.error("Kryo handler not found for exception {}", wingsException.getClass());
      return new KryoHandlerNotFoundException(wingsException.getMessage());
    }
    return wingsException;
  }

  private WingsException ensureExceptionIsKryoSerializable(WingsException wingsException) {
    if (wingsException == null) {
      return null;
    }

    WingsException kryoSerializedException = handleExceptionIfNotKryoRegistered(wingsException);
    try {
      setExceptionCause(
          kryoSerializedException, ensureExceptionIsKryoSerializable((WingsException) wingsException.getCause()));
    } catch (IllegalAccessException ignored) {
    }

    return kryoSerializedException;
  }
}
