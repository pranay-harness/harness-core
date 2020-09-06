package io.harness.engine.executables;

import com.google.inject.Inject;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.plan.PlanNode;
import io.harness.state.io.StepResponseNotifyData;
import io.harness.tasks.ResponseData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OwnedBy(HarnessTeam.CDC)
public class InvocationHelper {
  private final NodeExecutionService nodeExecutionService;

  @Inject
  public InvocationHelper(NodeExecutionService nodeExecutionService) {
    this.nodeExecutionService = nodeExecutionService;
  }

  public Map<String, ResponseData> accumulateResponses(String planExecutionId, String notifyId) {
    Map<String, ResponseData> response = new HashMap<>();
    List<NodeExecution> childExecutions = nodeExecutionService.fetchNodeExecutionsByNotifyId(planExecutionId, notifyId);
    for (NodeExecution childExecution : childExecutions) {
      PlanNode node = childExecution.getNode();
      StepResponseNotifyData notifyData = StepResponseNotifyData.builder()
                                              .nodeUuid(node.getUuid())
                                              .identifier(node.getIdentifier())
                                              .group(node.getGroup())
                                              .status(childExecution.getStatus())
                                              .failureInfo(childExecution.getFailureInfo())
                                              .stepOutcomeRefs(childExecution.getOutcomeRefs())
                                              .build();
      response.put(node.getUuid(), notifyData);
    }
    return response;
  }
}
