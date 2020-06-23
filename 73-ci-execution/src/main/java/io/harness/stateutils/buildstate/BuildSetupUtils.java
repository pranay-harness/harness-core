package io.harness.stateutils.buildstate;

import static io.harness.govern.Switch.unhandled;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ambiance.Ambiance;
import io.harness.beans.steps.stepinfo.BuildEnvSetupStepInfo;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.rest.RestResponse;
import lombok.extern.slf4j.Slf4j;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

@Singleton
@Slf4j
public class BuildSetupUtils {
  @Inject private K8BuildSetupUtils k8BuildSetupUtils;

  public RestResponse<K8sTaskExecutionResponse> executeCISetupTask(
      BuildEnvSetupStepInfo buildEnvSetupStepInfo, Ambiance ambiance) {
    switch (buildEnvSetupStepInfo.getSetupEnv().getBuildJobEnvInfo().getType()) {
      case K8:
        return k8BuildSetupUtils.executeCISetupTask(buildEnvSetupStepInfo, ambiance);
      default:
        unhandled(buildEnvSetupStepInfo.getSetupEnv().getBuildJobEnvInfo().getType());
    }
    return null;
  }

  public RestResponse<K8sTaskExecutionResponse> executeCILiteEngineTask(
      LiteEngineTaskStepInfo liteEngineTaskStepInfo, Ambiance ambiance) {
    switch (liteEngineTaskStepInfo.getEnvSetup().getBuildJobEnvInfo().getType()) {
      case K8:
        return k8BuildSetupUtils.executeK8sCILiteEngineTask(liteEngineTaskStepInfo, ambiance);
      default:
        unhandled(liteEngineTaskStepInfo.getEnvSetup().getBuildJobEnvInfo().getType());
    }
    return null;
  }
}
