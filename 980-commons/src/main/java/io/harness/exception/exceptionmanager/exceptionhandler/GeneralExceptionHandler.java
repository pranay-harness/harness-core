package io.harness.exception.exceptionmanager.exceptionhandler;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.GeneralException;
import io.harness.exception.WingsException;

import com.google.common.collect.ImmutableSet;
import java.util.Set;

@OwnedBy(HarnessTeam.DX)
public class GeneralExceptionHandler implements ExceptionHandler {
  public static Set<Class<? extends Exception>> exceptions() {
    return ImmutableSet.<Class<? extends Exception>>builder().add(NullPointerException.class).build();
  }

  @Override
  public WingsException handleException(Exception exception) {
    if (exception instanceof NullPointerException) {
      return new GeneralException("Null Pointer Exception");
    }
    return new GeneralException(exception.getMessage());
  }
}
