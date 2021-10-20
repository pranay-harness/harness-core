package io.harness.stateutils.buildstate;

import static io.harness.govern.Switch.unhandled;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.steps.stepinfo.InitializeStepInfo;
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

  public CIBuildSetupTaskParams getBuildSetupTaskParams(InitializeStepInfo initializeStepInfo, Ambiance ambiance,
      Map<String, String> taskIds, String logPrefix, Map<String, String> stepLogKeys) {
    switch (initializeStepInfo.getBuildJobEnvInfo().getType()) {
      case K8:
        return k8BuildSetupUtils.getCIk8BuildTaskParams(initializeStepInfo, ambiance, taskIds, logPrefix, stepLogKeys);
      default:
        unhandled(initializeStepInfo.getBuildJobEnvInfo().getType());
    }
    return null;
  }

  public List<ContainerDefinitionInfo> getBuildServiceContainers(InitializeStepInfo initializeStepInfo) {
    switch (initializeStepInfo.getBuildJobEnvInfo().getType()) {
      case K8:
        return k8BuildSetupUtils.getCIk8BuildServiceContainers(initializeStepInfo);
      default:
        unhandled(initializeStepInfo.getBuildJobEnvInfo().getType());
    }
    return null;
  }
}
