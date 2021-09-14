/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.yaml.errorhandling;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Transient;

@Data
@Builder
@FieldNameConstants(innerTypeName = "GitToHarnessErrorDetailsKeys")
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
