package io.harness.gitsync.interceptor;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.sdk.GitSyncApiConstants;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Getter
@Builder
@FieldNameConstants(innerTypeName = "GitEntityDeleteInfoKeys")
@OwnedBy(DX)
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "GitEntityDeleteInfo", description = "This contains details of the Git Entity for deletion")
public class GitEntityDeleteInfoDTO {
  @PathParam(GitSyncApiConstants.BRANCH_PARAM_MESSAGE) @QueryParam(GitSyncApiConstants.BRANCH_KEY) String branch;
  @PathParam(GitSyncApiConstants.REPOID_PARAM_MESSAGE)
  @QueryParam(GitSyncApiConstants.REPO_IDENTIFIER_KEY)
  String yamlGitConfigId;
  @PathParam(GitSyncApiConstants.FOLDER_PATH_PARAM_MESSAGE)
  @QueryParam(GitSyncApiConstants.FOLDER_PATH)
  String folderPath;
  @PathParam(GitSyncApiConstants.FILEPATH_PARAM_MESSAGE) @QueryParam(GitSyncApiConstants.FILE_PATH_KEY) String filePath;
  @PathParam(GitSyncApiConstants.COMMIT_MESSAGE_PARAM_MESSAGE)
  @QueryParam(GitSyncApiConstants.COMMIT_MSG_KEY)
  String commitMsg;
  @PathParam("Last Object Id") @QueryParam(GitSyncApiConstants.LAST_OBJECT_ID_KEY) String lastObjectId;
}
