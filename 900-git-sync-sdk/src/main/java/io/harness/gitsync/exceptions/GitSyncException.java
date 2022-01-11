/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.exceptions;

import static io.harness.eraro.ErrorCode.GIT_SYNC_ERROR;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

@OwnedBy(HarnessTeam.DX)
public class GitSyncException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public GitSyncException(String message) {
    super(message, null, GIT_SYNC_ERROR, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }
}
