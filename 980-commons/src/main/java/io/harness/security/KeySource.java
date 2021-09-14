/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Maps account Id to account key.
 */
@OwnedBy(PL)
@ParametersAreNonnullByDefault
public interface KeySource {
  /**
   * Returns the key for the given accountId, or {@code null} if it's not found.
   */
  @Nullable String fetchKey(String accountId);
}
