package io.harness.template.gitsync;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.manage.GlobalContextManager;
import io.harness.manage.GlobalContextManager.GlobalContextGuard;

@OwnedBy(CDC)
public class TemplateGitSyncBranchContextGuard implements AutoCloseable {
  private final GlobalContextGuard guard;

  public TemplateGitSyncBranchContextGuard(
      GitSyncBranchContext gitSyncBranchContext, boolean findDefaultFromOtherRepos) {
    this.guard = GlobalContextManager.initGlobalContextGuard(GlobalContextManager.obtainGlobalContextCopy());
    if (gitSyncBranchContext != null && gitSyncBranchContext.getGitBranchInfo() != null) {
      // Set findDefaultFromOtherBranches if it's not already true. This is done so that we can fetch entities used by
      // steps (like connectors) from default branch of other repos also.
      if (findDefaultFromOtherRepos && !gitSyncBranchContext.getGitBranchInfo().isFindDefaultFromOtherRepos()) {
        gitSyncBranchContext = gitSyncBranchContext.withGitBranchInfo(
            gitSyncBranchContext.getGitBranchInfo().withFindDefaultFromOtherRepos(true));
      }
      GlobalContextManager.upsertGlobalContextRecord(gitSyncBranchContext);
    } else {
      GlobalContextManager.upsertGlobalContextRecord(GitSyncBranchContext.builder().build());
    }
  }

  @Override
  public void close() {
    if (guard != null) {
      guard.close();
    }
  }
}
