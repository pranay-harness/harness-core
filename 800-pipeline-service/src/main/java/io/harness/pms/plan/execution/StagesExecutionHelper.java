package io.harness.pms.plan.execution;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.merger.helpers.InputSetMergeHelper;
import io.harness.pms.plan.execution.beans.StagesExecutionInfo;

import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
@UtilityClass
public class StagesExecutionHelper {
  public StagesExecutionInfo getStagesExecutionInfo(
      String pipelineYaml, List<String> stagesToRun, Map<String, String> expressionValues) {
    String pipelineToRun = InputSetMergeHelper.removeNonRequiredStages(pipelineYaml, stagesToRun);
    return StagesExecutionInfo.builder()
        .isStagesExecution(true)
        .pipelineYamlToRun(pipelineToRun)
        .fullPipelineYaml(pipelineYaml)
        .stageIdentifiers(stagesToRun)
        .expressionValues(expressionValues)
        .build();
  }
}
