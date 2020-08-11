package io.harness.engine.interrupts.helpers;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.ambiance.Ambiance;
import io.harness.ambiance.AmbianceUtils;
import io.harness.ambiance.Level;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.status.Status;
import io.harness.plan.PlanNode;
import io.harness.serializer.KryoSerializer;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@OwnedBy(CDC)
@Slf4j
public class RetryHelper {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEngine engine;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;
  @Inject private AmbianceUtils ambianceUtils;
  @Inject private KryoSerializer kryoSerializer;

  public void retryNodeExecution(String nodeExecutionId) {
    NodeExecution nodeExecution = Preconditions.checkNotNull(nodeExecutionService.get(nodeExecutionId));
    PlanNode node = nodeExecution.getNode();
    String newUuid = generateUuid();
    NodeExecution newNodeExecution = cloneForRetry(nodeExecution);
    Ambiance ambiance = ambianceUtils.cloneForFinish(nodeExecution.getAmbiance());
    ambiance.addLevel(Level.builder()
                          .setupId(node.getUuid())
                          .runtimeId(newUuid)
                          .stepType(node.getStepType())
                          .identifier(node.getIdentifier())
                          .group(node.getGroup())
                          .build());
    newNodeExecution.setUuid(newUuid);
    newNodeExecution.setAmbiance(ambiance);
    NodeExecution savedNodeExecution = nodeExecutionService.save(newNodeExecution);
    nodeExecutionService.updateRelationShipsForRetryNode(nodeExecution.getUuid(), savedNodeExecution.getUuid());
    nodeExecutionService.markRetried(nodeExecution.getUuid());
    executorService.submit(ExecutionEngineDispatcher.builder().ambiance(ambiance).orchestrationEngine(engine).build());
  }

  private NodeExecution cloneForRetry(NodeExecution nodeExecution) {
    NodeExecution newNodeExecution = kryoSerializer.clone(nodeExecution);
    newNodeExecution.setStartTs(null);
    newNodeExecution.setStatus(Status.QUEUED);
    List<String> retryIds = isEmpty(nodeExecution.getRetryIds()) ? new ArrayList<>() : nodeExecution.getRetryIds();
    retryIds.add(0, nodeExecution.getUuid());
    newNodeExecution.setRetryIds(retryIds);
    newNodeExecution.setExecutableResponses(new ArrayList<>());
    newNodeExecution.setVersion(null);
    return newNodeExecution;
  }
}
