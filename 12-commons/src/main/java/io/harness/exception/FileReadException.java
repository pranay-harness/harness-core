package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

public class FileReadException extends WingsException {
  public static final String MESSAGE_KEY = "message";
  public FileReadException(String message) {
    super(null, null, ErrorCode.GENERAL_ERROR, Level.ERROR, USER, null);
    super.param(MESSAGE_KEY, message);
  }
}
