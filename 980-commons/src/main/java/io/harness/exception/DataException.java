/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.DATA;

/**
 * This exception serves as super class for all exceptions to be used to store metadata for error
 * handling framework
 */
public abstract class DataException extends FrameworkBaseException {
  public DataException(Throwable cause) {
    super(cause, DATA);
  }
}
