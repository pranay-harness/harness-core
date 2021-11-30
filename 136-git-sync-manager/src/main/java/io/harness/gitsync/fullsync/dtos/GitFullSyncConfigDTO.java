package io.harness.gitsync.fullsync.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GitFullSyncConfigDTO {
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private String baseBranch;
  private String branch;
  private String message;
  private boolean createPullRequest;
  private String repoIdentifier;
}
