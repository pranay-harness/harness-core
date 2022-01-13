/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception.runtime;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.ErrorCode;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@OwnedBy(DX)
@EqualsAndHashCode(callSuper = true)
public class JGitRuntimeException extends RuntimeException {
  private String message;
  Throwable cause;
  ErrorCode code = ErrorCode.INVALID_CREDENTIAL;
  String commitId;
  String branch;

  public JGitRuntimeException(String message) {
    this.message = message;
  }

  public JGitRuntimeException(String message, ErrorCode code) {
    this.message = message;
    this.code = code;
  }

  public JGitRuntimeException(String message, Throwable cause) {
    this.message = message;
    this.cause = cause;
  }

  public JGitRuntimeException(String message, Throwable cause, ErrorCode code, String commitId, String branch) {
    this.message = message;
    this.cause = cause;
    this.code = code;
    this.commitId = commitId;
    this.branch = branch;
  }
}
