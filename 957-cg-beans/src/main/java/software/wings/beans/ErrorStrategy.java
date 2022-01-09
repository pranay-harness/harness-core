/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

/**
 *
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

/**
 * The enum Error strategy.
 *
 * @author Rishi
 */
@OwnedBy(CDC)
public enum ErrorStrategy {
  /**
   * Continue error strategy.
   */
  CONTINUE("Continue"),
  /**
   * Abort error strategy.
   */
  FAIL("Fail"),
  /**
   * Pause error strategy.
   */
  PAUSE("Pause"),
  /**
   * Retry error strategy.
   */
  RETRY("Retry");

  private String displayName;

  ErrorStrategy(String displayName) {
    this.displayName = displayName;
  }

  /**
   * Gets display name.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return displayName;
  }
}
