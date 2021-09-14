/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans;

import static io.harness.eraro.ErrorCode.DUPLICATE_DELEGATE_EXCEPTION;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class DuplicateDelegateException extends WingsException {
  private static final String DELEGATE_ID_PARAM = "delegateId";
  private static final String CONNECTION_ID_PARAM = "connectionId";

  public DuplicateDelegateException(String delegateId, String connectionId) {
    super(
        "Duplicate delegate with same delegateId exists", null, DUPLICATE_DELEGATE_EXCEPTION, Level.ERROR, USER, null);
    super.param(DELEGATE_ID_PARAM, delegateId);
    super.param(CONNECTION_ID_PARAM, connectionId);
  }
}
