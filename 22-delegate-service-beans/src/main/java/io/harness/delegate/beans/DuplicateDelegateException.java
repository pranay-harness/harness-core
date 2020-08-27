package io.harness.delegate.beans;

import static io.harness.eraro.ErrorCode.DUPLICATE_DELEGATE_EXCEPTION;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class DuplicateDelegateException extends WingsException {
  public DuplicateDelegateException() {
    super(
        "Duplicate delegate with same delegateId exists", null, DUPLICATE_DELEGATE_EXCEPTION, Level.ERROR, USER, null);
  }
}
