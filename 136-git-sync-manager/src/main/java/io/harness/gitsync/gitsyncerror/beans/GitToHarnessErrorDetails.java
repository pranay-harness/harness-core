package io.harness.gitsync.gitsyncerror.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Transient;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@FieldNameConstants(innerTypeName = "GitToHarnessErrorDetailsKeys")
@TypeAlias("io.harness.gitsync.gitsyncerror.beans.gitToHarnessErrorDetails")
@OwnedBy(DX)
public class GitToHarnessErrorDetails implements GitSyncErrorDetails {
  private String gitCommitId;
  private Long commitTime;
  private String yamlContent;
  private String commitMessage;
  @Transient private LatestErrorDetailForFile latestErrorDetailForFile;
  private List<String> previousCommitIdsWithError;
  private List<GitSyncError> previousErrors;

  @Data
  @Builder
  private static class LatestErrorDetailForFile {
    private String gitCommitId;
    private ChangeType changeType;
    private String failureReason;
  }

  public void populateCommitWithLatestErrorDetails(GitSyncError gitSyncError) {
    this.setLatestErrorDetailForFile(
        LatestErrorDetailForFile.builder()
            .gitCommitId(((GitToHarnessErrorDetails) gitSyncError.getAdditionalErrorDetails()).getGitCommitId())
            .changeType(gitSyncError.getChangeType())
            .failureReason(gitSyncError.getFailureReason())
            .build());
  }
}
