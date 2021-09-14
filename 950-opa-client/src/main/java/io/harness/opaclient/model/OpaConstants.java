/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.opaclient.model;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public interface OpaConstants {
  String OPA_EVALUATION_TYPE_PIPELINE = "Pipeline";
  String OPA_EVALUATION_TYPE_TF_PLAN = "TerraformPlan";
  String OPA_EVALUATION_TYPE_K8S_DRY_RUN = "K8sDryRun";

  String OPA_EVALUATION_ACTION_PIPELINE_EXECUTE = "Execute";
  String OPA_EVALUATION_ACTION_PIPELINE_CREATE = "Create";
  String OPA_EVALUATION_ACTION_PIPELINE_UPDATE = "Update";
}
