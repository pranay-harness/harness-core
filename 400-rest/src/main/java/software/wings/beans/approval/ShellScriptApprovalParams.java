/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.beans.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.Getter;
import lombok.Setter;

@OwnedBy(CDC)
public class ShellScriptApprovalParams {
  @Getter @Setter private String scriptString;

  /* Retry Interval in Milliseconds*/
  @Getter @Setter private Integer retryInterval;
}
