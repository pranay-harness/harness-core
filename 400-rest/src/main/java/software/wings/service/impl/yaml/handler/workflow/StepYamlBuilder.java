/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.yaml.ChangeContext;
import software.wings.yaml.workflow.StepYaml;

import java.util.Map;

@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
@OwnedBy(CDC)
public abstract class StepYamlBuilder {
  public static final String YAML_ID_LOG = "YAML_ID_LOGS: User sending id in yaml for stepType: {}, accountId: {}";
  public void validate(ChangeContext<StepYaml> changeContext) {
    // nothing here
  }

  public void convertIdToNameForKnownTypes(String name, Object objectValue, Map<String, Object> outputProperties,
      String appId, Map<String, Object> inputProperties) {
    outputProperties.put(name, objectValue);
  }

  public void convertNameToIdForKnownTypes(String name, Object objectValue, Map<String, Object> outputProperties,
      String appId, String accountId, Map<String, Object> inputProperties) {
    outputProperties.put(name, objectValue);
  }
}
