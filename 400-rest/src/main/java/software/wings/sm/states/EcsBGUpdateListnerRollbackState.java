/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.api.ContainerServiceElement;
import software.wings.helpers.ext.ecs.request.EcsListenerUpdateRequestConfigData;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(CDP)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class EcsBGUpdateListnerRollbackState extends EcsBGUpdateListnerState {
  public EcsBGUpdateListnerRollbackState(String name) {
    super(name, StateType.ECS_LISTENER_UPDATE_ROLLBACK.name());
  }

  @Override
  protected EcsListenerUpdateRequestConfigData getEcsListenerUpdateRequestConfigData(
      ContainerServiceElement containerServiceElement, ExecutionContext context) {
    EcsListenerUpdateRequestConfigData configData =
        super.getEcsListenerUpdateRequestConfigData(containerServiceElement, context);
    configData.setRollback(true);
    configData.setServiceNameDownsized(containerServiceElement.getEcsBGSetupData().getDownsizedServiceName());
    configData.setServiceCountDownsized(containerServiceElement.getEcsBGSetupData().getDownsizedServiceCount());
    configData.setTargetGroupForNewService(containerServiceElement.getTargetGroupForNewService());
    return configData;
  }
}
