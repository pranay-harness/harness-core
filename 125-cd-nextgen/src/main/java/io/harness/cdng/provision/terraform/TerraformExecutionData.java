package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.yaml.core.variables.NGVariable;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@OwnedBy(CDP)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TerraformExecutionData {
  String workspace;
  @JsonProperty("configFiles") TerraformConfigFilesWrapper terraformConfigFilesWrapper;
  @JsonProperty("varFiles") List<TerraformVarFile> terraformVarFiles;
  @JsonProperty("backendConfig") TerraformBackendConfig terraformBackendConfig;
  List<String> targets;
  List<NGVariable> environmentVariables;
}
