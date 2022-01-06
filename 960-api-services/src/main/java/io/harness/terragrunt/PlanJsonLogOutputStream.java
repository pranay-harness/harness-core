/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.terragrunt;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.EqualsAndHashCode;
import org.zeroturnaround.exec.stream.LogOutputStream;

@EqualsAndHashCode(callSuper = false)

@OwnedBy(CDP)
public class PlanJsonLogOutputStream extends LogOutputStream {
  private String planJson;

  @Override
  public void processLine(String line) {
    planJson = line;
  }

  public String getPlanJson() {
    return planJson;
  }
}
