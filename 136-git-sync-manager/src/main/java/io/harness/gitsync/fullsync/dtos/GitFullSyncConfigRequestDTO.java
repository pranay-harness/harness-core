/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.fullsync.dtos;

import io.harness.gitsync.sdk.GitSyncApiConstants;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@Schema(name = "GitFullSyncConfigRequest", description = "This contains details to trigger Full Sync")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GitFullSyncConfigRequestDTO {
  @Schema(description = "Branch on which Entities will be pushed") @NotNull String branch;
  @Schema(description = GitSyncApiConstants.REPOID_PARAM_MESSAGE) @NotNull String repoIdentifier;
  @Schema(description = "Root Folder Path where entities will be pushed") @NotNull String rootFolder;
  @Schema(description = "Checks the new Branch") boolean isNewBranch;
  @Schema(description = GitSyncApiConstants.DEFAULT_BRANCH_PARAM_MESSAGE) String baseBranch;
  @Schema(description = "This checks whether to create a pull request. Its default value is False")
  boolean createPullRequest;
  @Schema(description = "Target Branch for pull request") String targetBranch;
  @Schema(description = "PR Title") String prTitle;
}
