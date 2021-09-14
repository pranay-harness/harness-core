/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.pipeline;

import io.harness.annotations.dev.HarnessTeam;
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
import io.harness.cdng.provision.terraform.TerraformApplyStepInfo;
import io.harness.cdng.provision.terraform.TerraformDestroyStepInfo;
import io.harness.cdng.provision.terraform.TerraformPlanStepInfo;
import io.harness.cdng.provision.terraform.steps.rolllback.TerraformRollbackStepInfo;
import io.harness.plancreator.steps.common.WithStepElementParameters;
import io.harness.yaml.core.StepSpecType;

import io.swagger.annotations.ApiModel;

@ApiModel(
    subTypes = {K8sApplyStepInfo.class, K8sBlueGreenStepInfo.class, K8sCanaryStepInfo.class, K8sRollingStepInfo.class,
        K8sRollingRollbackStepInfo.class, K8sScaleStepInfo.class, ShellScriptStepInfo.class, K8sDeleteStepInfo.class,
        K8sBGSwapServicesStepInfo.class, K8sCanaryDeleteStepInfo.class, TerraformApplyStepInfo.class,
        TerraformPlanStepInfo.class, TerraformDestroyStepInfo.class, TerraformRollbackStepInfo.class})
@OwnedBy(HarnessTeam.CDC)
public interface CDStepInfo extends StepSpecType, WithStepElementParameters {}
