package io.harness.pms.plan.execution.beans.dto;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PIPELINE)
@Value
@Builder
@Schema(name = "RunStageRequest", description = "Request Parameters needed to run specific Stages of a Pipeline")
public class RunStageRequestDTO {
  String runtimeInputYaml;
  List<String> stageIdentifiers;
  Map<String, String> expressionValues;
}
