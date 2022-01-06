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
import software.wings.exception.IncompleteStateException;
import software.wings.yaml.workflow.StepYaml;

import org.apache.commons.lang3.StringUtils;

@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
@OwnedBy(CDC)
public class EmailStepYamlBuilder extends StepYamlBuilder {
  @Override
  public void validate(ChangeContext<StepYaml> changeContext) {
    StepYaml stepYaml = changeContext.getYaml();

    if (StringUtils.isBlank((String) stepYaml.getProperties().get("toAddress"))) {
      throw new IncompleteStateException("\"toAddress\" could not be empty or null, please provide a value.");
    } else if (StringUtils.isBlank((String) stepYaml.getProperties().get("subject"))) {
      throw new IncompleteStateException("\"subject\" could not be empty or null, please provide a value.");
    } else if (StringUtils.isBlank((String) stepYaml.getProperties().get("body"))) {
      throw new IncompleteStateException("\"body\" could not be empty or null, please provide a value.");
    }
  }
}
