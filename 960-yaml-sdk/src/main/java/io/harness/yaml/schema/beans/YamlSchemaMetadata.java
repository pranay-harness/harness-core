package io.harness.yaml.schema.beans;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@OwnedBy(HarnessTeam.PIPELINE)
@Data
@Builder
public class YamlSchemaMetadata {
  List<ModuleType> modulesSupported;
  @NotNull YamlGroup yamlGroup;
}
