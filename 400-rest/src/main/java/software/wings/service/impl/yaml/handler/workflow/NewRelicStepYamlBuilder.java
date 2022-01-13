/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.workflow;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.sm.StateType;

import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CV)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
@Slf4j
public class NewRelicStepYamlBuilder extends CVMetricStepYamlBuilder {
  @Override
  public StateType getStateType() {
    return StateType.NEW_RELIC;
  }
}
