package io.harness.engine.interrupts.helpers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.plan.PlanNodeUtils;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.LevelUtils;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class RetryHelper {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEngine engine;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;

  public void retryNodeExecution(String nodeExecutionId, StepParameters parameters) {
    NodeExecution nodeExecution = Preconditions.checkNotNull(nodeExecutionService.get(nodeExecutionId));
    PlanNodeProto node = nodeExecution.getNode();
    String newUuid = generateUuid();
    Ambiance ambiance = AmbianceUtils.cloneForFinish(nodeExecution.getAmbiance());
    ambiance = ambiance.toBuilder().addLevels(LevelUtils.buildLevelFromPlanNode(newUuid, node)).build();
    NodeExecution newNodeExecution = cloneForRetry(nodeExecution, parameters, newUuid, ambiance);
    NodeExecution savedNodeExecution = nodeExecutionService.save(newNodeExecution);
    nodeExecutionService.updateRelationShipsForRetryNode(nodeExecution.getUuid(), savedNodeExecution.getUuid());
    nodeExecutionService.markRetried(nodeExecution.getUuid());
    executorService.submit(ExecutionEngineDispatcher.builder().ambiance(ambiance).orchestrationEngine(engine).build());
  }

  private NodeExecution cloneForRetry(
      NodeExecution nodeExecution, StepParameters parameters, String newUuid, Ambiance ambiance) {
    PlanNodeProto newPlanNode = nodeExecution.getNode();
    if (parameters != null) {
      newPlanNode = PlanNodeUtils.cloneForRetry(nodeExecution.getNode(), parameters);
    }
    List<String> retryIds = isEmpty(nodeExecution.getRetryIds()) ? new ArrayList<>() : nodeExecution.getRetryIds();
    retryIds.add(0, nodeExecution.getUuid());
    return NodeExecution.builder()
        .uuid(newUuid)
        .ambiance(ambiance)
        .node(newPlanNode)
        .mode(null)
        .startTs(null)
        .endTs(null)
        .initialWaitDuration(null)
        .resolvedStepParameters((StepParameters) null)
        .notifyId(nodeExecution.getNotifyId())
        .parentId(nodeExecution.getParentId())
        .nextId(nodeExecution.getNextId())
        .previousId(nodeExecution.getPreviousId())
        .lastUpdatedAt(null)
        .version(null)
        .executableResponses(new ArrayList<>())
        .interruptHistories(nodeExecution.getInterruptHistories())
        .failureInfo(null)
        .status(Status.QUEUED)
        .timeoutInstanceIds(new ArrayList<>())
        .timeoutDetails(null)
        .outcomeRefs(new ArrayList<>())
        .retryIds(retryIds)
        .oldRetry(false)
        .build();
  }
}
