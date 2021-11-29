package io.harness.gitsync.interceptor;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.sdk.GitSyncApiConstants;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "GitEntityUpdateInfo", description = "This contains details of the Git Entity for update")
public class GitEntityUpdateInfoDTO {
  @Parameter(description = GitSyncApiConstants.BRANCH_PARAM_MESSAGE)
  @QueryParam(GitSyncApiConstants.BRANCH_KEY)
  String branch;
  @Parameter(description = GitSyncApiConstants.REPOID_PARAM_MESSAGE)
  @QueryParam(GitSyncApiConstants.REPO_IDENTIFIER_KEY)
  String yamlGitConfigId;
  @Parameter(description = GitSyncApiConstants.FOLDER_PATH_PARAM_MESSAGE)
  @QueryParam(GitSyncApiConstants.FOLDER_PATH)
  String folderPath;
  @Parameter(description = GitSyncApiConstants.FOLDER_PATH_PARAM_MESSAGE)
  @QueryParam(GitSyncApiConstants.FILE_PATH_KEY)
  String filePath;
  @Parameter(description = GitSyncApiConstants.COMMIT_MESSAGE_PARAM_MESSAGE)
  @QueryParam(GitSyncApiConstants.COMMIT_MSG_KEY)
  String commitMsg;
  @Parameter(description = "Last Object Id")
  @QueryParam(GitSyncApiConstants.LAST_OBJECT_ID_KEY)
  String lastObjectId; // required in case of update file
  @Parameter(description = GitSyncApiConstants.DEFAULT_BRANCH_PARAM_MESSAGE)
  @QueryParam(GitSyncApiConstants.BASE_BRANCH)
  String baseBranch;
}
