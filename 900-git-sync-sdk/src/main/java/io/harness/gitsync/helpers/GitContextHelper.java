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
  /***
   * In case of git sync, we will be updating the entity in a given repo and branch. To update the
   * entity we first need to fetch the entity from the repo. This utility is for the case when
   * someone selets create a new branch while updating an entity.
   *
   * The utility will return the repo and branch from where we should do the get call for get existing
   * entity.
   *
   * # Case 1
   *   If the user is updating the entity in the some repo1, branch1, then this utility returns repo1, branch1
   *
   * # Case 2
   *   If the user selects commit to a new branch from a base branch, then the existing entity won't be present
   *   in the new branch and we will have to fetch the entity from the base branch, thus we will retur repo, baseBranch
   ***/
  public static GitEntityInfo getGitInfoToGetExistingEntity() {
    GitEntityInfo gitEntityInfo = getGitEntityInfo();
    String repo;
    String branch;
    if (gitEntityInfo == null) {
      repo = null;
      branch = null;
    } else {
      repo = gitEntityInfo.getYamlGitConfigId();
      if (gitEntityInfo.isNewBranch()) {
        branch = gitEntityInfo.getBaseBranch();
      } else {
        branch = gitEntityInfo.getBranch();
      }
    }
    return GitEntityInfo.builder().yamlGitConfigId(repo).branch(branch).build();
  }

  public static GitEntityInfo getGitEntityInfo() {
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
}
