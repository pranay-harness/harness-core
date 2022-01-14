package io.harness.cdng.provision.terraform;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDP)
@RecasterAlias("io.harness.cdng.provision.terraform.TerraformPlanExecutionDataParameters")
public class TerraformPlanExecutionDataParameters {
  ParameterField<String> workspace;
  TerraformConfigFilesWrapper configFiles;
  LinkedHashMap<String, TerraformVarFile> varFiles;
  TerraformBackendConfig backendConfig;
  ParameterField<List<String>> targets;
  Map<String, Object> environmentVariables;
  TerraformPlanCommand command;
  ParameterField<String> secretManagerRef;
}
