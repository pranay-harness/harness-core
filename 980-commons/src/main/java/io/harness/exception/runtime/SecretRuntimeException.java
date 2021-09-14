/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
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
