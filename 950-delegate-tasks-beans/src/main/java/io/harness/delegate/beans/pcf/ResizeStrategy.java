/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
