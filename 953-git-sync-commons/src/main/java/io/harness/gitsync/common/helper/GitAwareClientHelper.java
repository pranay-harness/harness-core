/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.gitsync.interceptor.GitSyncConstants;
import io.harness.manage.GlobalContextManager;

import com.google.inject.Singleton;
import java.util.function.Supplier;

@Singleton
@OwnedBy(DX)
public class GitAwareClientHelper {
  public <T> T processGitAwareRequest(Supplier<T> supplier, String repo, String branch) {
    boolean isRepoNull = isEmpty(repo) || repo.equals(GitSyncConstants.DEFAULT);
    boolean isBranchNull = isEmpty(branch) || branch.equals(GitSyncConstants.DEFAULT);
    if (isRepoNull || isBranchNull) {
      return supplier.get();
    }
    final GitEntityInfo newBranch = GitEntityInfo.builder().branch(branch).yamlGitConfigId(repo).build();
    try (GlobalContextManager.GlobalContextGuard guard = GlobalContextManager.ensureGlobalContextGuard()) {
      GlobalContextManager.upsertGlobalContextRecord(GitSyncBranchContext.builder().gitBranchInfo(newBranch).build());
      return supplier.get();
    }
  }
}
