package io.harness.pms.pipeline.observer;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.plan.execution.beans.dto.GraphLayoutNodeDTO;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
public class OrchestrationObserverUtils {
  // Get list of all stage types that are executed
  public Set<String> getExecutedModulesInPipeline(PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity) {
    Set<String> executedModules = new HashSet<>();
    Map<String, GraphLayoutNodeDTO> layoutNodeMap = pipelineExecutionSummaryEntity.getLayoutNodeMap();
    for (GraphLayoutNodeDTO graphLayoutNode : layoutNodeMap.values()) {
      if (StatusUtils.isFinalStatus(graphLayoutNode.getStatus().getEngineStatus())) {
        if (graphLayoutNode.getSkipInfo() == null || !graphLayoutNode.getSkipInfo().getEvaluatedCondition()) {
          executedModules.add(graphLayoutNode.getModule());
        }
      }
    }
    return executedModules;
  }
}
