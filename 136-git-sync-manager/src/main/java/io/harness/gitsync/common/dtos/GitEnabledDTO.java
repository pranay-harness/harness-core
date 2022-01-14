package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Schema(name = "GitEnabled", description = "This contains details of connectivity mode and whether Git Sync is enabled")
@OwnedBy(DX)
public class GitEnabledDTO {
  @Schema(description = "This checks if Git Sync is enabled for a given scope") boolean isGitSyncEnabled;
  @Schema(description = "This is the Git Sync connectivity mode") ConnectivityMode connectivityMode;
}
