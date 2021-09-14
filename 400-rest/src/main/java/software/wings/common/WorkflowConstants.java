/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.common;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.regex.Pattern;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public interface WorkflowConstants {
  String K8S_DEPLOYMENT_ROLLING = "Rollout Deployment";
  String K8S_DEPLOYMENT_ROLLING_ROLLBACK = "Rollback Deployment";
  String K8S_BLUE_GREEN_DEPLOY = "Blue/Green Deployment";
  String K8S_SCALE = "Scale";
  String K8S_DELETE = "Delete";
  String K8S_CANARY_DEPLOY = "Canary Deployment";
  String K8S_STAGE_DEPLOY = "Stage Deployment";

  String K8S_PRIMARY_PHASE_NAME = "Primary";
  String K8S_CANARY_PHASE_NAME = "Canary";
  /**
   * The constant SUB_WORKFLOW.
   */
  String SUB_WORKFLOW = "SUB_WORKFLOW";
  /**
   * The constant SUB_WORKFLOW_ID.
   */
  String SUB_WORKFLOW_ID = "subWorkflowId";
  /**
   * The constant PHASE_NAME_PREFIX.
   */
  String PHASE_NAME_PREFIX = "Phase ";
  /**
   * The constant phaseNamePattern.
   */
  Pattern phaseNamePattern = Pattern.compile("Phase [0-9]+");
  /**
   * The constant STEP_VALIDATION_MESSAGE.
   */
  String STEP_VALIDATION_MESSAGE = "Some fields %s are found to be invalid/incomplete.";
  /**
   * The constant PHASE_STEP_VALIDATION_MESSAGE.
   */
  String PHASE_STEP_VALIDATION_MESSAGE = "Some steps %s are found to be invalid/incomplete.";
  /**
   * The constant WORKFLOW_VALIDATION_MESSAGE.
   */
  String WORKFLOW_VALIDATION_MESSAGE = "Some phases/steps %s are found to be invalid/incomplete.";
  /**
   * The constant WORKFLOW_ENV_INFRAMAPPING_VALIDATION_MESSAGE.
   */
  String WORKFLOW_ENV_VALIDATION_MESSAGE = "Environment is found to be invalid/incomplete.";
  /**
   * The constant WORKFLOW_ENV_INFRAMAPPING_VALIDATION_MESSAGE.
   */
  String WORKFLOW_INFRAMAPPING_VALIDATION_MESSAGE =
      "Some phases %s Service Infrastructure are found to be invalid/incomplete.";
  /**
   * The constant EXECUTE_WITH_PREVIOUS_STEPS.
   */
  String EXECUTE_WITH_PREVIOUS_STEPS = "executeWithPreviousSteps";
}
