/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.stepsdependency.constants;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(CDP)
@UtilityClass
public class OutcomeExpressionConstants {
  public final String SERVICE = "service";
  public final String ARTIFACTS = "artifacts";
  public final String MANIFESTS = "manifests";
  public final String INFRASTRUCTURE_OUTCOME = "stage.spec.infrastructure.output";
  public final String INFRASTRUCTURE_GROUP = "infrastructureGroup";
  public final String K8S_ROLL_OUT = "rollingOutcome";
  public final String K8S_BLUE_GREEN_OUTCOME = "k8sBlueGreenOutcome";
  public final String K8S_APPLY_OUTCOME = "k8sApplyOutcome";
  public final String K8S_CANARY_OUTCOME = "k8sCanaryOutcome";
  public final String K8S_CANARY_DELETE_OUTCOME = "k8sCanaryDeleteOutcome";
  public final String K8S_BG_SWAP_SERVICES_OUTCOME = "k8sBGSwapServicesOutcome";
  public final String OUTPUT = "output";
  public final String TERRAFORM_CONFIG = "terraformConfig";
  public final String DEPLOYMENT_INFO_OUTCOME = "deploymentInfoOutcome";
}
