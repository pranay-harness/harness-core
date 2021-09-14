/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.advisers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public enum CommonAdviserTypes {
  RETRY_WITH_ROLLBACK,
  MANUAL_INTERVENTION_WITH_ROLLBACK,
  ON_FAIL_ROLLBACK
}
