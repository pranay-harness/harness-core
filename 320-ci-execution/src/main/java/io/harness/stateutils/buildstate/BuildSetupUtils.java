package io.harness.stateutils.buildstate;

import static io.harness.govern.Switch.unhandled;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ambiance.Ambiance;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.delegate.beans.ci.CIBuildSetupTaskParams;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class BuildSetupUtils {
  @Inject private K8BuildSetupUtils k8BuildSetupUtils;

  public CIBuildSetupTaskParams getBuildSetupTaskParams(
      LiteEngineTaskStepInfo liteEngineTaskStepInfo, Ambiance ambiance) {
    switch (liteEngineTaskStepInfo.getBuildJobEnvInfo().getType()) {
      case K8:
        return k8BuildSetupUtils.getCIk8BuildTaskParams(liteEngineTaskStepInfo, ambiance);
      default:
        unhandled(liteEngineTaskStepInfo.getBuildJobEnvInfo().getType());
    }
    return null;
  }
}