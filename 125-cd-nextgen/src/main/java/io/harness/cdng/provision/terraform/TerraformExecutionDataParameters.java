package io.harness.cdng.provision.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDP)
public class TerraformExecutionDataParameters {
  ParameterField<String> workspace;
  TerraformConfigFilesWrapper configFiles;
  Map<String, TerraformVarFile> varFiles;
  TerraformBackendConfig backendConfig;
  ParameterField<List<String>> targets;
  Map<String, Object> environmentVariables;
}
