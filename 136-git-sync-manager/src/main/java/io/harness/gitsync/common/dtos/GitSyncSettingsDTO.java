package io.harness.gitsync.common.dtos;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.ORG_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.PROJECT_PARAM_MESSAGE;
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
  @Schema(description = ACCOUNT_PARAM_MESSAGE) @NotNull String accountIdentifier;
  @Schema(description = PROJECT_PARAM_MESSAGE) @NotNull String projectIdentifier;
  @Schema(description = ORG_PARAM_MESSAGE) @NotNull String organizationIdentifier;
  @Schema(
      description =
          "Specifies Connectivity Mode for Git Sync. If True, executes through Delegate, else executes through Platform. The default value is True")
  @NotNull
  boolean executeOnDelegate;
}
