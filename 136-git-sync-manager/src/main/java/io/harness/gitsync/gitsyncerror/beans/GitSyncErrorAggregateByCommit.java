/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.gitsyncerror.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "GitSyncErrorAggregateByCommitKeys")
@OwnedBy(PL)
public class GitSyncErrorAggregateByCommit {
  String gitCommitId;
  int failedCount;
  String repoId;
  String branchName;
  String commitMessage;
  long createdAt;
  List<GitSyncError> errorsForSummaryView;
}
