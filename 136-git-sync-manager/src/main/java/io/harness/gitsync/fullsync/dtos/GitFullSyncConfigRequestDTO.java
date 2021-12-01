package io.harness.gitsync.fullsync.dtos;

import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GitFullSyncConfigRequestDTO {
  @NotNull String branch;
  String message;
  String baseBranch;
  boolean createPullRequest;
  @NotNull String repoIdentifier;
}
