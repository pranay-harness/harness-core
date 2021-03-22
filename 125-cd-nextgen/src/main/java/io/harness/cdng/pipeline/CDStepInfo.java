package io.harness.cdng.pipeline;

import io.harness.cdng.k8s.K8sApplyStepInfo;
import io.harness.cdng.k8s.K8sBGSwapServicesStepInfo;
import io.harness.cdng.k8s.K8sBlueGreenStepInfo;
import io.harness.cdng.k8s.K8sCanaryDeleteStepInfo;
import io.harness.cdng.k8s.K8sCanaryStepInfo;
import io.harness.cdng.k8s.K8sDeleteStepInfo;
import io.harness.cdng.k8s.K8sRollingRollbackStepInfo;
import io.harness.cdng.k8s.K8sRollingStepInfo;
import io.harness.cdng.k8s.K8sScaleStepInfo;
import io.harness.plancreator.steps.shell.ShellScriptStepInfo;
import io.harness.executionplan.plancreator.beans.GenericStepInfo;
import io.harness.pms.sdk.core.steps.io.WithRollbackInfo;
import io.harness.yaml.core.StepSpecType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;

@ApiModel(
    subTypes = {K8sApplyStepInfo.class, K8sBlueGreenStepInfo.class, K8sCanaryStepInfo.class, K8sRollingStepInfo.class,
        K8sRollingRollbackStepInfo.class, K8sScaleStepInfo.class, ShellScriptStepInfo.class, K8sDeleteStepInfo.class,
        K8sBGSwapServicesStepInfo.class, K8sCanaryDeleteStepInfo.class})
public interface CDStepInfo extends GenericStepInfo, StepSpecType, WithRollbackInfo {
  @JsonIgnore String getIdentifier();
}
