package io.harness.gitsync.gitsyncerror.dtos;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(PL)
public class GitToHarnessErrorByCommitResponseDTO {
  @NotEmpty String gitCommitId;
  int failedCount;
  Long commitTime;
  String repoUrl;
  String branchName;
  String commitMessage;
  List<GitSyncErrorDTO> errorsForSummaryView;
}
