/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
