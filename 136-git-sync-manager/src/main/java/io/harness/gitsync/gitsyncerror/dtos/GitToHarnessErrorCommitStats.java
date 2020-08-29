package io.harness.gitsync.gitsyncerror.dtos;

import io.harness.gitsync.gitsyncerror.beans.GitSyncError;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.List;

@Data
@FieldNameConstants(innerTypeName = "GitToHarnessErrorCommitStatsKeys")
public class GitToHarnessErrorCommitStats {
  String gitCommitId;
  Integer failedCount;
  Long commitTime;
  String gitConnectorId;
  String branchName;
  String gitConnectorName;
  String commitMessage;
  List<GitSyncError> errorsForSummaryView;
}
