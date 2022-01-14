package io.harness.opaclient.model;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.PIPELINE)
public interface OpaConstants {
  String OPA_EVALUATION_TYPE_PIPELINE = "pipeline";
  String OPA_EVALUATION_ACTION_PIPELINE_RUN = "onrun";
  String OPA_EVALUATION_ACTION_PIPELINE_SAVE = "onsave";

  String OPA_STATUS_PASS = "pass";
  String OPA_STATUS_WARNING = "warning";
  String OPA_STATUS_ERROR = "error";
}
