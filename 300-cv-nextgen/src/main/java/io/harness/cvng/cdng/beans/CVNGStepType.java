/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.cdng.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonValue;

@OwnedBy(HarnessTeam.CV)
public enum CVNGStepType {
  CVNG_VERIFY("Continuous Verification", "Verify");
  private final String displayName;
  private final String folderPath;

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  CVNGStepType(String folderPath, String displayName) {
    this.folderPath = folderPath;
    this.displayName = displayName;
  }
  @JsonValue
  public String getFolderPath() {
    return this.folderPath;
  }
}
