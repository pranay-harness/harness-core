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
