package io.harness.gitsync.gitsyncerror.dtos;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

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
@OwnedBy(PL)
public class GitSyncErrorAggregateByCommitDTO {
  String gitCommitId;
  int failedCount;
  String repoId;
  String branchName;
  String commitMessage;
  long createdAt;
  List<GitSyncErrorDTO> errorsForSummaryView;
}
