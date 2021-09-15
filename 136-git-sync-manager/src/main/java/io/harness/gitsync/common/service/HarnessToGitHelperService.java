/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.common.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.schemas.entity.EntityScopeInfo;
import io.harness.gitsync.BranchDetails;
import io.harness.gitsync.FileInfo;
import io.harness.gitsync.PushFileResponse;
import io.harness.gitsync.PushInfo;
import io.harness.gitsync.RepoDetails;

@OwnedBy(DX)
public interface HarnessToGitHelperService {
  void postPushOperation(PushInfo pushInfo);

  Boolean isGitSyncEnabled(EntityScopeInfo entityScopeInfo);

  BranchDetails getBranchDetails(RepoDetails repoDetails);

  PushFileResponse pushFile(FileInfo request);
}
