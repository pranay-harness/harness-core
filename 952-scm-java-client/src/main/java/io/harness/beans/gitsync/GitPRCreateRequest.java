package io.harness.beans.gitsync;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.ORG_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.PROJECT_PARAM_MESSAGE;
import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@OwnedBy(DX)
public class GitPRCreateRequest {
  @Schema(description = "Branch on which changes are done") @NotEmpty @NotNull @Trimmed String sourceBranch;
  @Schema(description = "Branch on which changes need to be merged") @NotEmpty @NotNull @Trimmed String targetBranch;
  @Schema(description = "PR title") @NotEmpty @NotNull String title;
  @Schema(description = "Git Sync Config Id") @NotNull String yamlGitConfigRef;
  @Schema(description = ACCOUNT_PARAM_MESSAGE) @NotBlank String accountIdentifier;
  @Schema(description = ORG_PARAM_MESSAGE) String orgIdentifier;
  @Schema(description = PROJECT_PARAM_MESSAGE) String projectIdentifier;
  @Schema(
      description =
          "Specifies which token to use. If True, the SCM token will be used, else the Git Connector token will be used")
  boolean useUserFromToken;
}
