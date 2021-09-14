/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.pcf;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

/**
 * Created by brett on 9/20/17
 */
@OwnedBy(HarnessTeam.CDP)
public enum ResizeStrategy {
  RESIZE_NEW_FIRST("Resize New First"),
  DOWNSIZE_OLD_FIRST("Downsize Old First");

  private final String displayName;
  ResizeStrategy(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
