package io.harness.opaclient.model;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.PIPELINE)
public class OpaEvaluationResponseHolder {
  String id;
  String status;
  List<OpaPolicySetEvaluationResponse> details;
  String account_id;
  String org_id;
  String project_id;
  String entity;
  String type;
  String action;
  long created;
}
