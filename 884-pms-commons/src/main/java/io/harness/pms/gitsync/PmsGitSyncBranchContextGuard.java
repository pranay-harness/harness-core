package io.harness.pms.gitsync;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.manage.GlobalContextManager;
import io.harness.manage.GlobalContextManager.GlobalContextGuard;

@OwnedBy(PIPELINE)
public class PmsGitSyncBranchContextGuard implements AutoCloseable {
  private final GlobalContextGuard guard;

  public PmsGitSyncBranchContextGuard(GitSyncBranchContext gitSyncBranchContext, boolean findDefaultFromOtherBranches) {
    if (gitSyncBranchContext != null && gitSyncBranchContext.getGitBranchInfo() != null) {
      this.guard = GlobalContextManager.initGlobalContextGuard(GlobalContextManager.obtainGlobalContextCopy());

      // Set findDefaultFromOtherBranches if it's not already true. This is done so that we can fetch entities used by
      // steps (like connectors) from default branch of other repos also.
      if (findDefaultFromOtherBranches && !gitSyncBranchContext.getGitBranchInfo().isFindDefaultFromOtherBranches()) {
        gitSyncBranchContext = gitSyncBranchContext.withGitBranchInfo(
            gitSyncBranchContext.getGitBranchInfo().withFindDefaultFromOtherBranches(true));
      }
      GlobalContextManager.upsertGlobalContextRecord(gitSyncBranchContext);
    } else {
      this.guard = null;
    }
  }

  @Override
  public void close() {
    if (guard != null) {
      guard.close();
    }
  }
}
