package io.harness.cdng.pipeline;

import io.harness.cdng.k8s.K8sApplyStepInfo;
import io.harness.cdng.k8s.K8sBlueGreenStepInfo;
import io.harness.cdng.k8s.K8sCanaryStepInfo;
import io.harness.cdng.k8s.K8sRollingRollbackStepInfo;
import io.harness.cdng.k8s.K8sRollingStepInfo;
import io.harness.cdng.k8s.K8sScaleStepInfo;
import io.harness.cdng.pipeline.stepinfo.HttpStepInfo;
import io.harness.cdng.pipeline.stepinfo.ShellScriptStepInfo;
import io.harness.executionplan.plancreator.beans.GenericStepInfo;
import io.harness.pms.sdk.core.steps.io.WithRollbackInfo;
import io.harness.yaml.core.StepSpecType;

import io.swagger.annotations.ApiModel;

@ApiModel(
    subTypes = {K8sApplyStepInfo.class, K8sBlueGreenStepInfo.class, K8sCanaryStepInfo.class, K8sRollingStepInfo.class,
        K8sRollingRollbackStepInfo.class, K8sScaleStepInfo.class, HttpStepInfo.class, ShellScriptStepInfo.class})
public interface CDStepInfo extends GenericStepInfo, StepSpecType, WithRollbackInfo {}
