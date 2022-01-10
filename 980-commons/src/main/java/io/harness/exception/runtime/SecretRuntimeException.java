/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception.runtime;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Data;

@Data
@OwnedBy(HarnessTeam.PIPELINE)
public abstract class SecretRuntimeException extends RuntimeException {
  private String message;
  String secretRef;
  String scope;
  String connectorRef;
  Throwable cause;

  public SecretRuntimeException(String message) {
    this.message = message;
  }

  public SecretRuntimeException(String message, String secretRef, String scope, String connectorRef) {
    this.message = message;
    this.secretRef = secretRef;
    this.scope = scope;
    this.connectorRef = connectorRef;
  }

  public SecretRuntimeException(String message, Throwable cause) {
    this.message = message;
    this.cause = cause;
  }
}
