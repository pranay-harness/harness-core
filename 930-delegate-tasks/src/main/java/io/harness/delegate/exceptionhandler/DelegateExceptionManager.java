package io.harness.delegate.exceptionhandler;

import io.harness.delegate.exceptionhandler.handler.DelegateExceptionHandler;
import io.harness.exception.WingsException;

import com.google.inject.Singleton;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class DelegateExceptionManager {
  private static final ConcurrentMap<Class<? extends Exception>, DelegateExceptionHandler> exceptionHandlers =
      new ConcurrentHashMap<>();

  public static void registerHandler(
      Class<? extends Exception> exceptionClass, DelegateExceptionHandler delegateExceptionHandler) {
    if (delegateExceptionHandler != null && exceptionClass != null) {
      exceptionHandlers.putIfAbsent(exceptionClass, delegateExceptionHandler);
    } else {
      log.error("Invalid attempt to register delegate exception handler for exception : {} and delegate handler : {}",
          exceptionClass, delegateExceptionHandler);
    }
  }

  public static WingsException handle(Exception exception) {
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

  // ---------- PRIVATE METHODS -------------

  private static DelegateExceptionHandler getExceptionHandler(Exception exception) {
    return exceptionHandlers.getOrDefault(exception.getClass(), null);
  }

  private static WingsException prepareDefaultErrorResponse(Exception exception) {
    // TODO create new exception type - UnhandledException and use it here
    return new WingsException(exception);
  }
}
