package io.harness.gitsync.beans;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class GitFileChange extends Change {
  private String commitId;
  private String objectId;
  private String processingCommitId;
  private boolean changeFromAnotherCommit;
  private Long commitTimeMs;
  private Long processingCommitTimeMs;
  private String commitMessage;
  private String processingCommitMessage;
}
