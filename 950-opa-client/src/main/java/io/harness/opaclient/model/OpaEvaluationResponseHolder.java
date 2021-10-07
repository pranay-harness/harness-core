package io.harness.opaclient.model;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.PIPELINE)
public class OpaEvaluationResponseHolder {
  String id;
  OpaEvaluationResponse summary;
  String account_id;
  String org_id;
  String project_id;
}
