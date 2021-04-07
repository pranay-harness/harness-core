package io.harness.exception.exceptionmanager;

import static io.harness.exception.WingsException.ReportTarget.REST_API;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.DelegateErrorHandlerException;
import io.harness.exception.GeneralException;
import io.harness.exception.KryoHandlerNotFoundException;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.reflection.ReflectionUtils;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.DX)
public class ExceptionManager {
  @Inject private Map<Class<? extends Exception>, ExceptionHandler> exceptionHandler;
  @Inject private KryoSerializer kryoSerializer;

  public WingsException processException(Exception exception) {
    WingsException processedException = handleException(exception);
    return ensureExceptionIsKryoSerializable(processedException);
  }

  public List<ResponseMessage> buildResponseFromException(Exception exception) {
    WingsException processedException = processException(exception);
    return ExceptionLogger.getResponseMessageList(processedException, REST_API);
  }

  // ---------- PRIVATE METHODS -------------

  private ExceptionHandler getExceptionHandler(Exception exception) {
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
        ExceptionHandler exceptionHandler = getExceptionHandler(exception);
        if (exceptionHandler != null) {
          handledException = exceptionHandler.handleException(exception);
        } else {
          throw new DelegateErrorHandlerException("Exception handler not registered for exception : " + exception);
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
      log.error("Exception occured while handling exception : {}", exception, e);
      return prepareUnhandledExceptionResponse(exception);
    }
  }

  private WingsException prepareUnhandledExceptionResponse(Exception exception) {
    // default is to wrap unknown exception into wings exception using its message
    return new GeneralException(exception.getMessage());
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
