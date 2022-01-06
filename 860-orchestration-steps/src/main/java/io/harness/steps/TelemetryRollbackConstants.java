/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.steps;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public interface TelemetryRollbackConstants {
  // field values
  String TELEMETRY_ROLLBACK_PROP_VAL_UNASSIGNED = "N/A";

  // field names
  String TELEMETRY_ROLLBACK_PROP_PROJECT_ID = "projectId";
  String TELEMETRY_ROLLBACK_PROP_ORG_ID = "organizationId";
  String TELEMETRY_ROLLBACK_PROP_ACCOUNT_ID = "accountId";
  String TELEMETRY_ROLLBACK_PROP_ACCOUNT_NAME = "accountName";
  String TELEMETRY_ROLLBACK_PROP_EXECUTION_ID = "executionId";
  String TELEMETRY_ROLLBACK_PROP_PIPELINE_ID = "pipelineId";
  String TELEMETRY_ROLLBACK_PROP_STAGE_ID = "stageId";
  String TELEMETRY_ROLLBACK_PROP_STEP_ID = "stepId";
  String TELEMETRY_ROLLBACK_PROP_STATUS = "status";

  // telemetry event names
  String TELEMETRY_ROLLBACK_EXECUTION =
      "cd_rollback_execution"; // event sent for every rollback that happens during pipeline execution
}
