/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.USER_GROUP_ALREADY_EXIST;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

@OwnedBy(PL)
public class AccessRequestPresentException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public AccessRequestPresentException(String message) {
    super(message, null, USER_GROUP_ALREADY_EXIST, Level.INFO, null, null);
    super.param(MESSAGE_ARG, message);
  }
}
