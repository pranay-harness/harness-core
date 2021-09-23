package io.harness.pms.stages;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PIPELINE)
@Value
@Builder
public class StageExecutionResponse {
  String stageIdentifier;
  String stageName;
  String message;
  List<String> stagesRequired;
}
