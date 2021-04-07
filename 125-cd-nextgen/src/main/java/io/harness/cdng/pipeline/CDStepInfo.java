package io.harness.cdng.pipeline;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.k8s.K8sApplyStepInfo;
import io.harness.cdng.k8s.K8sBGSwapServicesStepInfo;
import io.harness.cdng.k8s.K8sBlueGreenStepInfo;
import io.harness.cdng.k8s.K8sCanaryDeleteStepInfo;
import io.harness.cdng.k8s.K8sCanaryStepInfo;
import io.harness.cdng.k8s.K8sDeleteStepInfo;
import io.harness.cdng.k8s.K8sRollingRollbackStepInfo;
import io.harness.cdng.k8s.K8sRollingStepInfo;
import io.harness.cdng.k8s.K8sScaleStepInfo;
import io.harness.cdng.pipeline.stepinfo.ShellScriptStepInfo;
import io.harness.executionplan.plancreator.beans.GenericStepInfo;
import io.harness.plancreator.steps.common.WithRollbackInfo;
import io.harness.yaml.core.StepSpecType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;

@ApiModel(
    subTypes = {K8sApplyStepInfo.class, K8sBlueGreenStepInfo.class, K8sCanaryStepInfo.class, K8sRollingStepInfo.class,
        K8sRollingRollbackStepInfo.class, K8sScaleStepInfo.class, ShellScriptStepInfo.class, K8sDeleteStepInfo.class,
        K8sBGSwapServicesStepInfo.class, K8sCanaryDeleteStepInfo.class})
@OwnedBy(CDC)
public interface CDStepInfo extends GenericStepInfo, StepSpecType, WithRollbackInfo {
  @JsonIgnore String getIdentifier();
}
