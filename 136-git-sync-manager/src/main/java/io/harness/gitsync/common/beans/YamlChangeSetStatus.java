/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.common.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import java.util.Arrays;
import java.util.List;

@OwnedBy(DX)
public enum YamlChangeSetStatus {
  QUEUED,
  RUNNING,
  FAILED,
  COMPLETED,
  SKIPPED,
  FAILED_WITH_RETRY;

  public static List<YamlChangeSetStatus> getTerminalStatusList() {
    return Arrays.asList(FAILED, COMPLETED, SKIPPED);
  }
}
