package io.harness.pms.plan.execution.beans;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.StagesExecutionMetadata;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PIPELINE)
@Value
@Builder
public class StagesExecutionInfo {
  boolean isStagesExecution;
  String pipelineYamlToRun;
  String fullPipelineYaml;
  List<String> stageIdentifiers;
  Map<String, String> expressionValues;

  public StagesExecutionMetadata toStagesExecutionMetadata() {
    return StagesExecutionMetadata.builder()
        .isStagesExecution(isStagesExecution)
        .fullPipelineYaml(fullPipelineYaml)
        .stageIdentifiers(stageIdentifiers)
        .expressionValues(expressionValues)
        .build();
  }
}
