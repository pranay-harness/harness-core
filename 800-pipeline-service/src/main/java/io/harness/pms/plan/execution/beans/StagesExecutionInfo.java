package io.harness.pms.plan.execution.beans;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.StagesExecutionMetadata;

import java.util.List;
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

  public StagesExecutionMetadata toStagesExecutionMetadata() {
    return StagesExecutionMetadata.builder()
        .isStagesExecution(isStagesExecution)
        .fullPipelineYaml(fullPipelineYaml)
        .stageIdentifiers(stageIdentifiers)
        .build();
  }
}
