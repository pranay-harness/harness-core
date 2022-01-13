/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.ENCRYPT_DECRYPT_ERROR;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import java.util.EnumSet;

/**
 * @author marklu on 8/31/19
 */
@OwnedBy(PL)
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
