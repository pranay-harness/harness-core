/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.KUBERNETES_YAML_ERROR;

import io.harness.eraro.Level;

public class KubernetesYamlException extends WingsException {
  private static final String REASON_ARG = "reason";

  public KubernetesYamlException(String reason) {
    this(reason, null);
  }

  public KubernetesYamlException(String reason, Throwable cause) {
    super(null, cause, KUBERNETES_YAML_ERROR, Level.ERROR, USER_SRE, null);
    super.param(REASON_ARG, reason);
  }
}
