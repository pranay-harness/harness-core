/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.gitsyncerror.dtos;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.sdk.GitSyncApiConstants;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@FieldNameConstants(innerTypeName = "GitSyncErrorAggregateByCommitDTOKeys")
@Schema(name = "GitSyncErrorAggregateByCommit",
    description = "This contains a list of Git Sync Error details for a given Commit Id")
@OwnedBy(PL)
public class GitSyncErrorAggregateByCommitDTO {
  @Schema(description = "Commit Id") String gitCommitId;
  @Schema(description = "The number of active errors in a commit") int failedCount;
  @Schema(description = GitSyncApiConstants.REPOID_PARAM_MESSAGE) String repoId;
  @Schema(description = GitSyncApiConstants.BRANCH_PARAM_MESSAGE) String branchName;
  @Schema(description = GitSyncApiConstants.COMMIT_MESSAGE_PARAM_MESSAGE) String commitMessage;
  @Schema(description = "This is the time at which the Git Sync error was logged") long createdAt;
  @Schema(description = "This has the list of Git Sync errors corresponding to a specific Commit Id")
  List<GitSyncErrorDTO> errorsForSummaryView;
}
