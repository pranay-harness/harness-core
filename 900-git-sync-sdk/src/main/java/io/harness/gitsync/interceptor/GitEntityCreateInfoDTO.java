/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

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
@FieldNameConstants(innerTypeName = "GitEntityCreateInfoKeys")
@OwnedBy(DX)
@NoArgsConstructor
@AllArgsConstructor
public class GitEntityCreateInfoDTO {
  @QueryParam(GitSyncApiConstants.BRANCH_KEY) String branch;
  @QueryParam(GitSyncApiConstants.REPO_IDENTIFIER_KEY) String yamlGitConfigId;
  @QueryParam(GitSyncApiConstants.FOLDER_PATH) String folderPath;
  @QueryParam(GitSyncApiConstants.FILE_PATH_KEY) String filePath;
  @QueryParam(GitSyncApiConstants.COMMIT_MSG_KEY) String commitMsg;
  @QueryParam(GitSyncApiConstants.NEW_BRANCH) boolean isNewBranch;
  @QueryParam(GitSyncApiConstants.BASE_BRANCH) String baseBranch;
}
