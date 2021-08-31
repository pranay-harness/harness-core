package io.harness.cvng.cdng.services.impl;

import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;

public class CVNGStepUtils {
  public static final String INFRASTRUCTURE_KEY = "infrastructure";
  public static final String SERVICE_CONFIG_KEY = "serviceConfig";
  public static final String SERVICE_REF_KEY = "serviceRef";
  public static final String ENVIRONMENT_REF_KEY = "environmentRef";
  public static final String SPEC_KEY = "spec";
  public static final String STAGE_KEY = "stage";
  public static final String EXECUTION_KEY = "execution";

  public static YamlNode getServiceRefNode(YamlNode stageYaml) {
    return stageYaml.getField(SPEC_KEY)
        .getNode()
        .getField(SERVICE_CONFIG_KEY)
        .getNode()
        .getField(SERVICE_REF_KEY)
        .getNode();
  }

  public static YamlNode getEnvRefNode(YamlNode stageYaml) {
    return stageYaml.getField(SPEC_KEY)
        .getNode()
        .getField(INFRASTRUCTURE_KEY)
        .getNode()
        .getField(ENVIRONMENT_REF_KEY)
        .getNode();
  }

  public static YamlField getExecutionNodeField(YamlNode stageYaml) {
    return stageYaml.getField(SPEC_KEY).getNode().getField(EXECUTION_KEY);
  }
}
