package io.harness.beans.gitsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Trimmed;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;

@Value
@Builder
@OwnedBy(DX)
public class GitPRCreateRequest {
  @NotEmpty @NotNull @Trimmed String sourceBranch;
  @NotEmpty @NotNull @Trimmed String targetBranch;
  @NotEmpty @NotNull String title;
  @NotNull String yamlGitConfigRef;
  @NotBlank String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  boolean useUserFromToken;
}
