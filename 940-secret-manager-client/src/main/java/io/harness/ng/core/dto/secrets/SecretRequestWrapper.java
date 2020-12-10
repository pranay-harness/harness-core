package io.harness.ng.core.dto.secrets;

import io.harness.EntityType;
import io.harness.yamlSchema.YamlSchemaRoot;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@YamlSchemaRoot(EntityType.SECRETS)
public class SecretRequestWrapper {
  @Valid @NotNull private SecretDTOV2 secret;
}
