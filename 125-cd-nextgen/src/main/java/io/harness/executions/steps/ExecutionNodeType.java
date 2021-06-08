package io.harness.executions.steps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.visitor.YamlTypes;

@OwnedBy(HarnessTeam.CDP)
public enum ExecutionNodeType {
  SERVICE("SERVICE", YamlTypes.SERVICE_ENTITY),
  SERVICE_CONFIG("SERVICE_CONFIG", YamlTypes.SERVICE_CONFIG),
  SERVICE_DEFINITION("SERVICE_DEFINITION", YamlTypes.SERVICE_DEFINITION),
  SERVICE_SPEC("SERVICE_SPEC", YamlTypes.SERVICE_SPEC),
  ARTIFACTS("ARTIFACTS", "artifacts"),
  ARTIFACT("ARTIFACT", "artifact"),
  SIDECARS("SIDECARS", "sidecars"),
  MANIFESTS("MANIFESTS", "manifests"),
  MANIFEST_FETCH("MANIFEST_FETCH", YamlTypes.MANIFEST_LIST_CONFIG),
  MANIFEST("MANIFEST", YamlTypes.MANIFEST_CONFIG),
  PIPELINE_SETUP("PIPELINE_SETUP", "pipeline"),
  INFRASTRUCTURE_SECTION("INFRASTRUCTURE_SECTION", YamlTypes.PIPELINE_INFRASTRUCTURE),
  INFRASTRUCTURE("INFRASTRUCTURE", YamlTypes.INFRASTRUCTURE_DEF),
  DEPLOYMENT_STAGE_STEP("DEPLOYMENT_STAGE_STEP", "deployment"),
  K8S_ROLLING("K8S_ROLLING", YamlTypes.K8S_ROLLING_DEPLOY),
  K8S_ROLLBACK_ROLLING("K8S_ROLLBACK_ROLLING", YamlTypes.K8S_ROLLING_ROLLBACK),
  K8S_BLUE_GREEN("K8S_BLUE_GREEN", YamlTypes.K8S_BLUE_GREEN_DEPLOY),
  K8S_APPLY("K8S_APPLY", YamlTypes.K8S_APPLY),
  K8S_SCALE("K8S_SCALE", YamlTypes.K8S_SCALE),
  K8S_CANARY("K8S_CANARY", YamlTypes.K8S_CANARY_DEPLOY),
  K8S_BG_SWAP_SERVICES("K8S_BG_SWAP_SERVICES", YamlTypes.K8S_BG_SWAP_SERVICES),
  K8S_DELETE("K8S_DELETE", YamlTypes.K8S_DELETE),
  K8S_CANARY_DELETE("K8S_CANARY_DELETE", YamlTypes.K8S_CANARY_DELETE),

  ROLLBACK_SECTION("ROLLBACK_SECTION", "rollbackSection"),
  GENERIC_SECTION("GENERIC_SECTION", "genericSection"),
  SHELL_SCRIPT("SHELL_SCRIPT", YamlTypes.SHELL_SCRIPT_STEP),
  TERRAFORM_APPLY("TERRAFORM_APPLY", StepSpecTypeConstants.TERRAFORM_APPLY),
  TERRAFORM_PLAN("TERRAFORM_PLAN", StepSpecTypeConstants.TERRAFORM_PLAN),
  TERRAFORM_DESTROY("TERRAFORM_DESTROY", StepSpecTypeConstants.TERRAFORM_DESTROY),
  TERRAFORM_ROLLBACK("TERRAFORM_ROLLBACK", StepSpecTypeConstants.TERRAFORM_ROLLBACK),

  AWS_SAM_DEPLOY("AWS_SAM_DEPLOY", StepSpecTypeConstants.AWS_SAM_DEPLOY);

  private final String name;
  private final String yamlType;

  ExecutionNodeType(String name, String yamlType) {
    this.name = name;
    this.yamlType = yamlType;
  }

  public String getName() {
    return name;
  }

  public String getYamlType() {
    return yamlType;
  }
}
