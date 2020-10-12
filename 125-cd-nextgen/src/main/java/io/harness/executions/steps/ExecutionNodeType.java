package io.harness.executions.steps;

import io.harness.cdng.visitor.YamlTypes;

public enum ExecutionNodeType {
  SERVICE("SERVICE", YamlTypes.SERVICE_CONFIG),
  MANIFEST_FETCH("MANIFEST_FETCH", YamlTypes.MANIFEST_LIST_CONFIG),
  PIPELINE_SETUP("PIPELINE_SETUP", "pipeline"),
  INFRASTRUCTURE_SECTION("INFRASTRUCTURE_SECTION", YamlTypes.PIPELINE_INFRASTRUCTURE),
  INFRASTRUCTURE("INFRASTRUCTURE", YamlTypes.INFRASTRUCTURE_DEF),
  DEPLOYMENT_STAGE_STEP("DEPLOYMENT_STAGE_STEP", "deployment"),
  K8S_ROLLING("K8S_ROLLING", YamlTypes.K8S_ROLLING_DEPLOY),
  K8S_ROLLBACK_ROLLING("K8S_ROLLBACK_ROLLING", YamlTypes.K8S_ROLLING_ROLLBACK),
  ROLLBACK_SECTION("ROLLBACK_SECTION", "rollback"),
  GENERIC_SECTION("GENERIC_SECTION", "genericSection");

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
