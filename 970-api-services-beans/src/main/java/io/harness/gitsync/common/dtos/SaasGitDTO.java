package io.harness.gitsync.common.dtos;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(HarnessTeam.DX)
@Value
@Builder
@FieldNameConstants(innerTypeName = "SaasGitDTOKeys")
@Schema(name = "SaasGit", description = "This contains a boolean which specifies whether the repoURL is SaasGit or not")
public class SaasGitDTO {
  @NotNull boolean isSaasGit;
}
