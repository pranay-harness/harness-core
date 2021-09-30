package io.harness.execution;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PIPELINE)
@Value
@Builder
public class StagesExecutionMetadata {
  boolean isStagesExecution;
  String fullPipelineYaml;
  List<String> stageIdentifiers;
}
