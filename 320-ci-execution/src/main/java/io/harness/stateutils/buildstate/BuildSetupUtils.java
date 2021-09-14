/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.stateutils.buildstate;

import static io.harness.govern.Switch.unhandled;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.delegate.beans.ci.CIBuildSetupTaskParams;
import io.harness.pms.contracts.ambiance.Ambiance;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.CI)
public class BuildSetupUtils {
  @Inject private K8BuildSetupUtils k8BuildSetupUtils;

  public CIBuildSetupTaskParams getBuildSetupTaskParams(LiteEngineTaskStepInfo liteEngineTaskStepInfo,
      Ambiance ambiance, Map<String, String> taskIds, String logPrefix, Map<String, String> stepLogKeys) {
    switch (liteEngineTaskStepInfo.getBuildJobEnvInfo().getType()) {
      case K8:
        return k8BuildSetupUtils.getCIk8BuildTaskParams(
            liteEngineTaskStepInfo, ambiance, taskIds, logPrefix, stepLogKeys);
      default:
        unhandled(liteEngineTaskStepInfo.getBuildJobEnvInfo().getType());
    }
    return null;
  }

  public List<ContainerDefinitionInfo> getBuildServiceContainers(LiteEngineTaskStepInfo liteEngineTaskStepInfo) {
    switch (liteEngineTaskStepInfo.getBuildJobEnvInfo().getType()) {
      case K8:
        return k8BuildSetupUtils.getCIk8BuildServiceContainers(liteEngineTaskStepInfo);
      default:
        unhandled(liteEngineTaskStepInfo.getBuildJobEnvInfo().getType());
    }
    return null;
  }
}
