/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.DEL)
public enum SelectorType {
  PROFILE_NAME,
  DELEGATE_NAME,
  HOST_NAME,
  GROUP_NAME,
  GROUP_SELECTORS,
  PROFILE_SELECTORS
}
