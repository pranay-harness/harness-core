package software.wings.yaml.errorhandling;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Transient;

import java.util.List;

@Data
@Builder
@FieldNameConstants(innerTypeName = "GitToHarnessErrorDetailsKeys")
public class GitToHarnessErrorDetails implements GitSyncErrorDetails {
  private String gitCommitId;
  private Long commitTime;
  private String yamlContent;
  @Transient private LatestErrorDetailForFile latestErrorDetailForFile;
  private List<String> previousCommitIdsWithError;
  private List<GitSyncError> previousErrors;

  @Data
  @Builder
  private static class LatestErrorDetailForFile {
    private String gitCommitId;
    private String changeType;
    private String failureReason;
  }

  public void populateCommitWithLatestErrorDetails(GitSyncError gitSyncError) {
    this.setLatestErrorDetailForFile(
        latestErrorDetailForFile.builder()
            .gitCommitId(((GitToHarnessErrorDetails) gitSyncError.getAdditionalErrorDetails()).getGitCommitId())
            .changeType(gitSyncError.getChangeType())
            .failureReason(gitSyncError.getFailureReason())
            .build());
  }
}
