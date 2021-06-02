package io.harness.ng.core.dto.secrets;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.beans.YamlDTO;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@OwnedBy(PL)
@Data
@Builder
public class SecretRequestWrapper implements YamlDTO {
  @Valid @NotNull private SecretDTOV2 secret;
}
