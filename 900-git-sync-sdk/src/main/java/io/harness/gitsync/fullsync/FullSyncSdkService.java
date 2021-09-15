/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.fullsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.FileChanges;
import io.harness.gitsync.ScopeDetails;

@OwnedBy(DX)
public interface FullSyncSdkService {
  FileChanges getFileChanges(ScopeDetails scopeDetails);
}
