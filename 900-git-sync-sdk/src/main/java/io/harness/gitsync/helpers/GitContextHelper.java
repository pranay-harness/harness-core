/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.helpers;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.gitsync.interceptor.GitSyncConstants.DEFAULT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.manage.GlobalContextManager;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(DX)
public class GitContextHelper {
  public GitEntityInfo getGitEntityInfo() {
    final GitSyncBranchContext gitSyncBranchContext =
        GlobalContextManager.get(GitSyncBranchContext.NG_GIT_SYNC_CONTEXT);
    if (gitSyncBranchContext == null) {
      log.error("Git branch context set as null even git sync is enabled");
      // Setting to default branch in case it is not set.
      return null;
    }
    GitEntityInfo gitBranchInfo = gitSyncBranchContext.getGitBranchInfo();
    if (gitBranchInfo == null || gitBranchInfo.getYamlGitConfigId() == null || gitBranchInfo.getBranch() == null
        || gitBranchInfo.getYamlGitConfigId().equals(DEFAULT) || gitBranchInfo.getBranch().equals(DEFAULT)) {
      return null;
    }
    return gitBranchInfo;
  }

  public boolean isUpdateToNewBranch() {
    GitEntityInfo gitEntityInfo = getGitEntityInfo();
    if (gitEntityInfo == null) {
      return false;
    }
    return gitEntityInfo.isNewBranch();
  }
}
