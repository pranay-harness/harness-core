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
@FieldNameConstants(innerTypeName = "GitToHarnessErrorByCommitKeys")
@OwnedBy(PL)
public class GitToHarnessErrorByCommitResponseDTO {
  String gitCommitId;
  int failedCount;
  Long commitTime;
  String repoId;
  String branchName;
  String commitMessage;
  List<GitSyncErrorDTO> errorsForSummaryView;
}
