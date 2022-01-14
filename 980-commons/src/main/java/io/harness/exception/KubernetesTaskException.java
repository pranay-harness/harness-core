/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.KUBERNETES_API_TASK_EXCEPTION;

import io.harness.eraro.Level;

import java.util.EnumSet;

public class KubernetesTaskException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public KubernetesTaskException(String message) {
    super(message, null, KUBERNETES_API_TASK_EXCEPTION, Level.ERROR, null, EnumSet.of(FailureType.APPLICATION_ERROR));
    super.param(MESSAGE_ARG, message);
  }

  public KubernetesTaskException(String message, FailureType failureType) {
    super(message, null, KUBERNETES_API_TASK_EXCEPTION, Level.ERROR, null, EnumSet.of(failureType));
    super.param(MESSAGE_ARG, message);
  }

  public KubernetesTaskException(String message, Throwable cause) {
    super(message, cause, KUBERNETES_API_TASK_EXCEPTION, Level.ERROR, null, EnumSet.of(FailureType.APPLICATION_ERROR));
    super.param(MESSAGE_ARG, message);
  }
}
