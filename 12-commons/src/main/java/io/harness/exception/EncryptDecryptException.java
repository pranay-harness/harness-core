package io.harness.exception;

import static io.harness.eraro.ErrorCode.ENCRYPT_DECRYPT_ERROR;

import io.harness.eraro.Level;

import java.util.EnumSet;

/**
 * @author marklu on 8/31/19
 */
public class EncryptDecryptException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public EncryptDecryptException(String message) {
    super(message, null, ENCRYPT_DECRYPT_ERROR, Level.ERROR, null, null);
    super.param(MESSAGE_KEY, message);
  }

  public EncryptDecryptException(String message, Throwable cause) {
    super(message, cause, ENCRYPT_DECRYPT_ERROR, Level.ERROR, null, null);
    super.param(MESSAGE_KEY, message);
  }

  public EncryptDecryptException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, ENCRYPT_DECRYPT_ERROR, Level.ERROR, reportTargets, null);
    super.param(MESSAGE_KEY, message);
  }

  public EncryptDecryptException(String message, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(message, cause, ENCRYPT_DECRYPT_ERROR, Level.ERROR, reportTargets, null);
    super.param(MESSAGE_KEY, message);
  }
}
