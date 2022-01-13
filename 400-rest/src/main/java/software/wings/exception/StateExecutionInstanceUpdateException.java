/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.exception;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

@OwnedBy(HarnessTeam.CDC)
// This is special exception to handle ticket: https://harness.atlassian.net/browse/CDC-15969.
// Do not use this exception anywhere else.
public class StateExecutionInstanceUpdateException extends WingsException {
  public StateExecutionInstanceUpdateException(String message) {
    super(message, null, null, Level.ERROR, null, null);
  }
}
