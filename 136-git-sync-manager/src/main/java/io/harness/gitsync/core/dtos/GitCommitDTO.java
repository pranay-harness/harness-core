package io.harness.gitsync.core.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.GitSyncDirection;
import io.harness.gitsync.core.beans.GitCommit.FailureReason;
import io.harness.gitsync.core.beans.GitCommit.GitCommitProcessingStatus;
import io.harness.gitsync.gitfileactivity.beans.GitFileProcessingSummary;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(DX)
public class GitCommitDTO {
  private String accountIdentifier;
  private String commitId;
  private GitCommitProcessingStatus status;
  private FailureReason failureReason;
  private GitFileProcessingSummary fileProcessingSummary;
  private GitSyncDirection gitSyncDirection;
  private String commitMessage;
  private String repoURL;
  private String branchName;
  private long lastUpdatedAt;
}
