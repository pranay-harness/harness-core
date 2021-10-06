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
  List<GitSyncError> errorsForSummaryView;
}
