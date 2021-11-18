package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "GitSyncSettings", description = "This contains details of Git Sync Settings")
@OwnedBy(DX)
public class GitSyncSettingsDTO {
  @NotNull String accountIdentifier;
  @NotNull String projectIdentifier;
  @NotNull String organizationIdentifier;
  @NotNull boolean executeOnDelegate;
}
