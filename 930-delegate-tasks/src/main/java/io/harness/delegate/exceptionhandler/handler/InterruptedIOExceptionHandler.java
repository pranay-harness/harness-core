/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.exceptionhandler.handler;

import io.harness.exception.ConnectException;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;

import com.google.common.collect.ImmutableSet;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.Set;
import org.apache.http.conn.ConnectTimeoutException;

public class InterruptedIOExceptionHandler implements ExceptionHandler {
  public static Set<Class<? extends Exception>> exceptions() {
    return ImmutableSet.<Class<? extends Exception>>builder().add(InterruptedIOException.class).build();
  }

  @Override
  public WingsException handleException(Exception exception) {
    // handler to handle SocketTimeoutException and ConnectTimeoutException
    InterruptedIOException interruptedIOException = (InterruptedIOException) exception;
    if (interruptedIOException instanceof SocketTimeoutException) {
      return new HintException("Please ensure requested url server is running",
          new ConnectException(interruptedIOException.getMessage(), WingsException.USER));
    } else if (interruptedIOException instanceof ConnectTimeoutException) {
      return new ExplanationException(
          String.format("While trying to make http request to %s , I got connect timeout exception",
              ((ConnectTimeoutException) exception).getHost()),
          new HintException(String.format("Please ensure that url %s is reachable from delegate",
                                ((ConnectTimeoutException) interruptedIOException).getHost().getHostName()),
              new HintException("Check your firewall rules",
                  new ConnectException(interruptedIOException.getMessage(), WingsException.USER))));
    }
    return new ConnectException(interruptedIOException.getMessage(), WingsException.USER);
  }
}
