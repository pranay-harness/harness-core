/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
