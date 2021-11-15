package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Schema(name = "GitEnabled", description = "This contains details of mode of connectivity and Git Sync Enablement")
@OwnedBy(DX)
public class GitEnabledDTO {
  boolean isGitSyncEnabled;
  ConnectivityMode connectivityMode;
}
