package io.harness.gitsync.interceptor;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.sdk.GitSyncApiConstants;

import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Getter
@Builder
@FieldNameConstants(innerTypeName = "GitEntityUpdateInfoKeys")
@OwnedBy(DX)
@NoArgsConstructor
@AllArgsConstructor
public class GitEntityUpdateInfoDTO {
  @QueryParam(GitSyncApiConstants.BRANCH_KEY) String branch;
  @QueryParam(GitSyncApiConstants.REPO_IDENTIFIER_KEY) String yamlGitConfigId;
  @QueryParam(GitSyncApiConstants.FOLDER_PATH) String folderPath;
  @QueryParam(GitSyncApiConstants.FILE_PATH_KEY) String filePath;
  @QueryParam(GitSyncApiConstants.COMMIT_MSG_KEY) String commitMsg;
  @QueryParam(GitSyncApiConstants.CREATE_PR_KEY) boolean createPr;
  @QueryParam(GitSyncApiConstants.LAST_OBJECT_ID_KEY) String lastObjectId; // required in case of update file
  @QueryParam(GitSyncApiConstants.TARGET_BRANCH_FOR_PR) String targetBranchForPr;
  @QueryParam(GitSyncApiConstants.BASE_BRANCH) String baseBranch;
}
