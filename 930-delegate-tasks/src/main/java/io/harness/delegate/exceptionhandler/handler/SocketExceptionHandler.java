/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.exceptionhandler.handler;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ConnectException;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import java.net.SocketException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.HttpHostConnectException;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class SocketExceptionHandler implements ExceptionHandler {
  public static Set<Class<? extends Exception>> exceptions() {
    return ImmutableSet.<Class<? extends Exception>>builder().add(SocketException.class).build();
  }

  @Override
  public WingsException handleException(Exception exception) {
    SocketException ex = (SocketException) exception;
    if (ex instanceof HttpHostConnectException) {
      return new ExplanationException(String.format("While trying to make http request %s I got this error!!",
                                          ((HttpHostConnectException) ex).getHost()),
          new HintException("Please make sure you are having internet connectivity",
              new HintException(
                  "Check your firewall rules", new ConnectException(ex.getMessage(), WingsException.USER))));
    } else if (ex instanceof java.net.ConnectException) {
      return new ConnectException(ex.getMessage(), WingsException.USER);
    }

    return new ConnectException(ex.getMessage(), WingsException.USER);
  }
}
