/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.validation.Validator.notNullCheck;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ResourceConstraint;

import software.wings.service.intfc.ResourceConstraintService;

import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
@OwnedBy(CDC)
@Slf4j
public class ResourceConstraintStepYamlBuilder extends StepYamlBuilder {
  private static final String RESOURCE_CONSTRAINT_ID = "resourceConstraintId";
  private static final String RESOURCE_CONSTRAINT_NAME = "resourceConstraintName";

  @Inject ResourceConstraintService resourceConstraintService;

  @Override
  public void convertIdToNameForKnownTypes(String name, Object objectValue, Map<String, Object> outputProperties,
      String appId, Map<String, Object> inputProperties) {
    if (RESOURCE_CONSTRAINT_ID.equals(name)) {
      ResourceConstraint resourceConstraint = resourceConstraintService.getById((String) objectValue);
      notNullCheck("Resource constraint does not exist.", resourceConstraint);
      outputProperties.put(RESOURCE_CONSTRAINT_NAME, resourceConstraint.getName());
      return;
    }
    outputProperties.put(name, objectValue);
  }

  @Override
  public void convertNameToIdForKnownTypes(String name, Object objectValue, Map<String, Object> outputProperties,
      String appId, String accountId, Map<String, Object> inputProperties) {
    if (RESOURCE_CONSTRAINT_NAME.equals(name)) {
      String resourceConstraintName = (String) objectValue;
      ResourceConstraint resourceConstraint = resourceConstraintService.getByName(accountId, resourceConstraintName);
      notNullCheck(String.format("Resource constraint %s does not exist.", resourceConstraintName), resourceConstraint);
      outputProperties.put(RESOURCE_CONSTRAINT_ID, resourceConstraint.getUuid());
      return;
    }
    if (RESOURCE_CONSTRAINT_ID.equals(name)) {
      log.info(YAML_ID_LOG, "RESOURCE CONSTRAINT", accountId);
    }
    outputProperties.put(name, objectValue);
  }
}
