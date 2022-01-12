/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.instrumentaion;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineInstrumentationConstants {
  public static String PIPELINE_EXECUTION = "ng_pipeline_execution";
  public static String PIPELINE_NOTIFICATION = "ng_pipeline_notification";
  public static String EXECUTION_TIME = "execution_time";
  public static String LEVEL = "level";
  public static String STAGE_TYPES = "stage_types";
  public static String STEP_TYPES = "step_types";
  public static String TRIGGER_TYPE = "trigger_type";
  public static String STATUS = "status";
  public static String IS_RERUN = "is_rerun";
  public static String CONDITIONAL_EXECUTION = "conditional_execution";
  public static String STAGE_COUNT = "stage_count";
  public static String STEP_COUNT = "step_count";
  public static String NOTIFICATION_METHODS = "notification_methods";
  public static String NOTIFICATION_RULES_COUNT = "notification_rules_count";
  public static String EVENT_TYPES = "events_types";
  public static String FAILURE_TYPES = "failure_types";
  public static String ERROR_MESSAGES = "error_messages";
  public static String ACCOUNT_NAME = "account_name";
}
