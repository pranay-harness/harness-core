/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(DX)
public interface GitSyncSdkService {
  /**
   * Gets details like repo and branch from context.
   */
  boolean isGitSyncEnabled(String accountIdentifier, String orgIdentifier, String projectIdentifier);

  /**
   * Gets details like repo and branch from context.
   */
  boolean isDefaultBranch(String accountIdentifier, String orgIdentifier, String projectIdentifier);
}
