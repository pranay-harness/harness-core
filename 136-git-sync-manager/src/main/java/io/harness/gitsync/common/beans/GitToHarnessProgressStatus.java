/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.common.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Arrays;
import java.util.List;

@OwnedBy(HarnessTeam.DX)
public enum GitToHarnessProgressStatus {
  TO_DO,
  IN_PROCESS,
  ERROR,
  DONE,
  ;

  public boolean isFailureStatus() {
    return terminalFailureStatusList().contains(this);
  }

  public boolean isInProcess() {
    return inProcessStatusList().contains(this);
  }

  public boolean isSuccessStatus() {
    return terminalSuccessStatusList().contains(this);
  }

  private List<GitToHarnessProgressStatus> terminalFailureStatusList() {
    return Arrays.asList(ERROR);
  }

  private List<GitToHarnessProgressStatus> terminalSuccessStatusList() {
    return Arrays.asList(DONE);
  }

  private List<GitToHarnessProgressStatus> inProcessStatusList() {
    return Arrays.asList(TO_DO, IN_PROCESS);
  }
}
