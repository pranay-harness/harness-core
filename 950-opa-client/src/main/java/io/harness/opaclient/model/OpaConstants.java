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
