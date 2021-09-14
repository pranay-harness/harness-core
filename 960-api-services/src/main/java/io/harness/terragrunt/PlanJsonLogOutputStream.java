/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
