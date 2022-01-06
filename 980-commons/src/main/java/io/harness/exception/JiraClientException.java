/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.JIRA_CLIENT_ERROR;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import lombok.Getter;

@OwnedBy(HarnessTeam.CDC)
public class JiraClientException extends WingsException {
  @Getter private final boolean fatal;

  public JiraClientException(String message, boolean fatal, Throwable cause) {
    super(message, cause, JIRA_CLIENT_ERROR, Level.ERROR, null, null);
    super.param("message", message);
    this.fatal = fatal;
  }

  public JiraClientException(String message, Throwable cause) {
    this(message, false, cause);
  }

  public JiraClientException(String message, boolean fatal) {
    this(message, fatal, null);
  }

  public JiraClientException(String message) {
    this(message, null);
  }
}
