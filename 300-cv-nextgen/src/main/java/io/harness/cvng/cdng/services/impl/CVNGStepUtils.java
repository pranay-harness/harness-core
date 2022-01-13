/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
  public static final String STAGES_KEY = "stages";
  public static final String EXECUTION_KEY = "execution";
  public static final String USE_FROM_STAGE_KEY = "useFromStage";

  public static YamlNode getServiceRefNode(YamlNode stageYaml) {
    return stageYaml.getField(SPEC_KEY)
        .getNode()
        .getField(SERVICE_CONFIG_KEY)
        .getNode()
        .getField(SERVICE_REF_KEY)
        .getNode();
  }

  public static boolean hasServiceIdentifier(YamlNode stageYaml) {
    return stageYaml.getField(SPEC_KEY).getNode().getField(SERVICE_CONFIG_KEY).getNode().getField(SERVICE_REF_KEY)
        != null;
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
