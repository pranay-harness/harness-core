/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

/**
 * This exception is used to add context data to the error to be used for logging in error framework
 *
 * It does make a copy of current MDC context and additionally supports list of key-value pairs
 * to be added to context
 */
@OwnedBy(HarnessTeam.DX)
public class ContextException extends FrameworkBaseException {
  public ContextException(Throwable cause) {
    super(cause, ErrorCode.CONTEXT);
  }

  public ContextException(Throwable cause, List<Pair<String, String>> contextInfo) {
    super(cause, ErrorCode.CONTEXT);
    if (contextInfo != null) {
      for (Pair<String, String> contextPair : contextInfo) {
        context(contextPair.getKey(), contextPair.getValue());
      }
    }
  }
}
