package io.harness.gitsync.gitsyncerror.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;

import java.util.List;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants(innerTypeName = "GitToHarnessErrorCommitStatsKeys")
@OwnedBy(DX)
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
