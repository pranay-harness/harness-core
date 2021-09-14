/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public enum ExecutionStatusCategory {
  SUCCEEDED("Succeeded"),
  ERROR("Error"),
  ACTIVE("Active");

  private String displayName;
  ExecutionStatusCategory(String s) {
    displayName = s;
  }

  public String getDisplayName() {
    return displayName;
  }
}
