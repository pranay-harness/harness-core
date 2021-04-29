package io.harness.gitsync.sdk;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(DX)
public class GitSyncApiConstants {
  public static final String BRANCH_KEY = "branch";
  public static final String REPO_IDENTIFIER_KEY = "repoIdentifier";
  public static final String FILE_PATH_KEY = "filePath";
  public static final String COMMIT_MSG_KEY = "commitMsg";
  public static final String CREATE_PR_KEY = "createPr";
  public static final String LAST_OBJECT_ID_KEY = "lastObjectId";
  public static final String FOLDER_PATH = "rootFolder";
  public static final String NEW_BRANCH = "isNewBranch";
  public static final String TARGET_BRANCH_FOR_PR = "targetBranchForPr";
}
