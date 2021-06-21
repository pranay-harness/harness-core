package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(DX)
public class GitSyncSettingsDTO {
  @NotNull String accountIdentifier;
  @NotNull String projectIdentifier;
  @NotNull String organizationIdentifier;
  @NotNull boolean executeOnDelegate;
}
