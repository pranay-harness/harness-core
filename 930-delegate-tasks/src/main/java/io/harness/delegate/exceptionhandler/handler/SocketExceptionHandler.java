package io.harness.delegate.exceptionhandler.handler;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ConnectException;
import io.harness.exception.WingsException;
import io.harness.exception.exceptionmanager.exceptionhandler.ExceptionHandler;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Singleton;
import java.net.SocketException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

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
    return new ConnectException(ex.getMessage(), WingsException.USER);
  }
}
