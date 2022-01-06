/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.service.impl.aws.model;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.CDP)
public interface AwsConstants {
  String AWS_SIMPLE_HTTP_CONNECTIVITY_URL = "https://aws.amazon.com/";
  int DEFAULT_AMI_ASG_MAX_INSTANCES = 10;
  int DEFAULT_AMI_ASG_MIN_INSTANCES = 0;
  int DEFAULT_AMI_ASG_DESIRED_INSTANCES = 6;
  int DEFAULT_AMI_ASG_TIMEOUT_MIN = 10;
  String DEFAULT_AMI_ASG_NAME = "DEFAULT_ASG";
  String AMI_SETUP_COMMAND_NAME = "AMI Service Setup";
  String AWS_AMI_ALL_PHASE_ROLLBACK_NAME = "Aws Ami All Phase Rollback";
  String PHASE_PARAM = "PHASE_PARAM";
  String FORWARD_LISTENER_ACTION = "forward";

  // Alb Traffic Shift Constants
  int MAX_TRAFFIC_SHIFT_WEIGHT = 100;
  int MIN_TRAFFIC_SHIFT_WEIGHT = 0;
  int DEFAULT_TRAFFIC_SHIFT_WEIGHT = 10;

  // AMI Sweeping output constants
  String AMI_SERVICE_SETUP_SWEEPING_OUTPUT_NAME = "setupSweepingOutputAmi";
  String AMI_ALB_SETUP_SWEEPING_OUTPUT_NAME = "setupSweepingOutputAmiAlb";

  // Main Ecs Container Name
  String MAIN_ECS_CONTAINER_NAME_TAG = "HARNESS_DEPLOYED_MAIN_CONTAINER";

  // ECS Sweeping output constants
  String ECS_SERVICE_SETUP_SWEEPING_OUTPUT_NAME = "setupSweepingOutputEcs";
  String ECS_SERVICE_DEPLOY_SWEEPING_OUTPUT_NAME = "deploySweepingOutputEcs";
  String ECS_RUN_TASK_DEPLOY_SWEEPING_OUTPUT_NAME = "runTaskDeploySweepingOutputEcs";
  String ECS_ALL_PHASE_ROLLBACK_DONE = "ecsAllPhaseRollbackDone";

  String MIN_INSTANCES = "minInstancesExpr";
  String MAX_INSTANCES = "maxInstancesExpr";
  String DESIRED_INSTANCES = "targetInstancesExpr";
  String AUTO_SCALING_TIMEOUT = "autoScalingSteadyStateTimeout";

  String VARIABLE_ACTIVITY_ID = "activityId";
  String DISPLAY_ACTIVITY_ID = "Activity Id";
  String AUTOSCALING_GROUP = "AutoScaling Group";
  String OLD_AUTOSCALING_GROUP = "Old AutoScaling Group";
  String NEW_AUTOSCALING_GROUP = "New AutoScaling Group";
  String NEW_AUTOSCALING_GROUP_WEIGHT = "New AutoScaling Group Weight";

  String UP_SCALE_ASG_COMMAND_UNIT = "Upscale AutoScaling Group";
  String DOWN_SCALE_ASG_COMMAND_UNIT = "Downscale AutoScaling Group";

  int LAMBDA_SLEEP_SECS = 5;
  String ECS_STEADY_STATE_CHECK = "ECS Steady State Check";
  String ECS_RUN_TASK = "ECS Run Task";

  String PROD_LISTENER = "Prod Listener";
  String STAGE_LISTENER = "Stage Listener";

  String CONTEXT_NEW_ASG_NAME_EXPR = "ami.newAsgName";
  String CONTEXT_OLD_ASG_NAME_EXPR = "ami.oldAsgName";
  String AWS_DEFAULT_REGION = "us-east-1";

  int DEFAULT_STATE_TIMEOUT_BUFFER_MIN = 5;

  int DEFAULT_BACKOFF_MAX_ERROR_RETRIES = 5;
}
