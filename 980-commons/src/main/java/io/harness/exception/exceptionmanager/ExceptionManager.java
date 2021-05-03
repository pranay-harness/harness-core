package io.harness.exception.exceptionmanager;

import static io.harness.exception.WingsException.ReportTarget.REST_API;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.ExplanationException;
import io.harness.exception.GeneralException;
import io.harness.exception.HintException;
import io.harness.exception.KryoHandlerNotFoundException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;
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

  public final String DEFAULT_ERROR_MESSAGE = "NULL EXCEPTION";

  public WingsException processException(Exception exception) {
    try {
      WingsException processedException = handleException(exception);
      return ensureExceptionIsKryoSerializable(processedException);
    } catch (Exception ex) {
      log.error("Exception occured while handling error in exception manager", ex);
      return NestedExceptionUtils.hintWithExplanationException(HintException.HINT_UNEXPECTED_ERROR,
          ExplanationException.EXPLANATION_UNEXPECTED_ERROR, new GeneralException(ex.getMessage()));
    }
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
    if (exception == null) {
      return new GeneralException(DEFAULT_ERROR_MESSAGE);
    }

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
          log.error("Exception handler not registered for exception : ", exception);
          handledException = prepareUnhandledExceptionResponse(exception);
        }
        WingsException cascadedException = handledException;
        while (cascadedException.getCause() != null) {
          // 3rd party exception can't be allowed as cause in already handled exception
          cascadedException = (WingsException) cascadedException.getCause();
        }
        setExceptionStacktrace(cascadedException, exception.getStackTrace());
        if (exception.getCause() != null) {
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

  private void setExceptionStacktrace(WingsException exception, StackTraceElement[] stackTraceElements)
      throws IllegalAccessException {
    ReflectionUtils.setObjectField(
        ReflectionUtils.getFieldByName(exception.getClass(), "stackTrace"), exception, stackTraceElements);
  }

  private boolean isExceptionKryoRegistered(WingsException wingsException) {
    return kryoSerializer.isRegistered(wingsException.getClass());
  }

  private WingsException handleExceptionIfNotKryoRegistered(Exception exception) {
    if (!(exception instanceof WingsException) || !isExceptionKryoRegistered((WingsException) exception)) {
      log.error("Kryo handler not found for exception {}", exception.getClass());
      return new KryoHandlerNotFoundException(exception.getMessage());
    }
    return (WingsException) exception;
  }

  private WingsException ensureExceptionIsKryoSerializable(Exception exception) {
    if (exception == null) {
      return null;
    }

    if (!(exception instanceof WingsException)) {
      log.error("Unknown runtime exception found in processed exception : {}", exception.getClass());
    }

    WingsException kryoSerializedException = handleExceptionIfNotKryoRegistered(exception);
    try {
      setExceptionCause(kryoSerializedException, ensureExceptionIsKryoSerializable((Exception) exception.getCause()));
    } catch (IllegalAccessException ignored) {
    }

    return kryoSerializedException;
  }
}
