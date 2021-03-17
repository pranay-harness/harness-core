package io.harness.ng.core.dto.secrets;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SecretRequestWrapper {
  @Valid @NotNull private SecretDTOV2 secret;
}
