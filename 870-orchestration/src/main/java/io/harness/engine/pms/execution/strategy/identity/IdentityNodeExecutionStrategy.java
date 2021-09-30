package io.harness.engine.pms.execution.strategy.identity;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.springdata.SpringDataMongoUtils.setUnset;

import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.pms.advise.AdviseHandlerFactory;
import io.harness.engine.pms.advise.AdviserResponseHandler;
import io.harness.engine.pms.commons.events.PmsEventSender;
import io.harness.engine.pms.execution.strategy.NodeExecutionStrategy;
import io.harness.engine.utils.PmsLevelUtils;
import io.harness.execution.ExecutionModeUtils;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.logging.AutoLogContext;
import io.harness.plan.IdentityPlanNode;
import io.harness.plan.PlanNode;
import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.start.NodeStartEvent;
import io.harness.pms.events.base.PmsEventCategory;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.steps.io.StepResponseNotifyData;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import io.harness.waiter.WaitNotifyEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Update;

@Slf4j
public class IdentityNodeExecutionStrategy implements NodeExecutionStrategy<IdentityPlanNode>{
  @Inject private PmsEventSender eventSender;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private AdviseHandlerFactory adviseHandlerFactory;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private OrchestrationEngine orchestrationEngine;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;

  @Override
  public void triggerNode(Ambiance ambiance, IdentityPlanNode node) {
    String uuid = generateUuid();
    NodeExecution previousNodeExecution = null;
    if (AmbianceUtils.obtainCurrentRuntimeId(ambiance) != null) {
      previousNodeExecution = nodeExecutionService.update(AmbianceUtils.obtainCurrentRuntimeId(ambiance),
          ops -> ops.set(NodeExecutionKeys.nextId, uuid).set(NodeExecutionKeys.endTs, System.currentTimeMillis()));
    }
    Ambiance cloned = AmbianceUtils.cloneForFinish(ambiance, PmsLevelUtils.buildLevelFromNode(uuid, node));
    NodeExecution nodeExecution =
        NodeExecution.builder()
            .uuid(uuid)
            .planNode(node)
            .ambiance(cloned)
            .levelCount(cloned.getLevelsCount())
            .status(Status.QUEUED)
            .notifyId(previousNodeExecution == null ? null : previousNodeExecution.getNotifyId())
            .parentId(previousNodeExecution == null ? null : previousNodeExecution.getParentId())
            .previousId(previousNodeExecution == null ? null : previousNodeExecution.getUuid())
            .unitProgresses(new ArrayList<>())
            .startTs(AmbianceUtils.getCurrentLevelStartTs(cloned))
            .build();
    nodeExecutionService.save(nodeExecution);
    // TODO: Should add to an execution queue rather than submitting straight to thread pool
    executorService.submit(() -> startExecution(cloned));
  }

  @Override
  public void startExecution(Ambiance ambiance) {
    String newNodeExecutionId = Objects.requireNonNull(AmbianceUtils.obtainCurrentRuntimeId(ambiance));
    NodeExecution newNodeExecution = nodeExecutionService.get(AmbianceUtils.obtainCurrentRuntimeId(ambiance));
    IdentityPlanNode node = newNodeExecution.getNode();
    try (AutoLogContext ignore = AmbianceUtils.autoLogContext(ambiance)) {
      NodeExecution originalExecution = nodeExecutionService.get(node.getOriginalNodeExecutionId());
      Update ops = new Update();
      setUnset(ops, NodeExecutionKeys.resolvedStepParameters, originalExecution.getResolvedStepParameters());
      setUnset(ops, NodeExecutionKeys.resolvedInputs, originalExecution.getResolvedInputs());
      setUnset(ops, NodeExecutionKeys.mode, originalExecution.getMode());
      setUnset(ops, NodeExecutionKeys.nodeRunInfo, originalExecution.getNodeRunInfo());
      setUnset(ops, NodeExecutionKeys.skipInfo, originalExecution.getSkipInfo());
      setUnset(ops, NodeExecutionKeys.failureInfo, originalExecution.getFailureInfo());
      setUnset(ops, NodeExecutionKeys.progressData, originalExecution.getProgressData());
      setUnset(ops, NodeExecutionKeys.adviserResponse, originalExecution.getAdviserResponse());
      setUnset(ops, NodeExecutionKeys.timeoutInstanceIds, originalExecution.getTimeoutInstanceIds());
      setUnset(ops, NodeExecutionKeys.timeoutDetails, originalExecution.getTimeoutDetails());
      setUnset(ops, NodeExecutionKeys.adviserTimeoutInstanceIds, originalExecution.getAdviserTimeoutInstanceIds());
      setUnset(ops, NodeExecutionKeys.adviserTimeoutDetails, originalExecution.getAdviserTimeoutDetails());
      setUnset(ops, NodeExecutionKeys.interruptHistories, originalExecution.getInterruptHistories());

      // If Node is skipped then call the adviser response handler straight away
      if (originalExecution.getStatus() == Status.SKIPPED) {
        Consumer<Update> updateConsumer = op -> new Update();
        updateConsumer.accept(ops);
        nodeExecutionService.updateStatusWithOps(
            newNodeExecutionId, Status.SKIPPED, updateConsumer, EnumSet.noneOf(Status.class));
        processAdviserResponse(ambiance, originalExecution.getAdviserResponse());
        return;
      }

      // If this is one of the leaf modes then just clone and copy everything and we should be good
      // This is an optimization/hack to not do any actual work
      if (ExecutionModeUtils.isLeafMode(newNodeExecution.getMode())) {
        // TODO: Copy outputs
        // TODO: Copy outcomes
        // TODO: Update outcome refs
        // TODO: Update Status to old status and call processAdvisorResponse
        return;
      }

      // If not leaf node then we need to call the identity step
      NodeStartEvent nodeStartEvent = NodeStartEvent.newBuilder()
                                          .setAmbiance(newNodeExecution.getAmbiance())
                                          .setStepParameters(ByteString.copyFromUtf8(node.getStepParameters().toJson()))
                                          .setMode(newNodeExecution.getMode())
                                          .build();
      eventSender.sendEvent(newNodeExecution.getAmbiance(), nodeStartEvent.toByteString(), PmsEventCategory.NODE_START,
          node.getServiceName(), true);
    } catch (Exception exception) {
      log.error("Exception Occurred in facilitateAndStartStep NodeExecutionId : {}, PlanExecutionId: {}",
          AmbianceUtils.obtainCurrentRuntimeId(ambiance), ambiance.getPlanExecutionId(), exception);
      handleError(ambiance, exception);
    }
  }

  @Override
  public void processAdviserResponse(Ambiance ambiance, AdviserResponse adviserResponse) {
    try (AutoLogContext ignore = AmbianceUtils.autoLogContext(ambiance)) {
      String nodeExecutionId = Objects.requireNonNull(AmbianceUtils.obtainCurrentRuntimeId(ambiance));
      if (adviserResponse == null || adviserResponse.getType() == AdviseType.UNKNOWN) {
        endNodeExecution(ambiance);
        return;
      }
      log.info("Starting to handle Adviser Response of type: {}", adviserResponse.getType());
      NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
      AdviserResponseHandler adviserResponseHandler = adviseHandlerFactory.obtainHandler(adviserResponse.getType());
      adviserResponseHandler.handleAdvise(nodeExecution, adviserResponse);
    }
  }

  @Override
  public void endNodeExecution(Ambiance ambiance) {
    String nodeExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    NodeExecution nodeExecution = nodeExecutionService.update(
        nodeExecutionId, ops -> ops.set(NodeExecutionKeys.endTs, System.currentTimeMillis()));
    if (isNotEmpty(nodeExecution.getNotifyId())) {
      PlanNode planNode = nodeExecution.getNode();
      StepResponseNotifyData responseData = StepResponseNotifyData.builder()
                                                .nodeUuid(planNode.getUuid())
                                                .stepOutcomeRefs(nodeExecution.getOutcomeRefs())
                                                .failureInfo(nodeExecution.getFailureInfo())
                                                .identifier(planNode.getIdentifier())
                                                .group(planNode.getGroup())
                                                .status(nodeExecution.getStatus())
                                                .adviserResponse(nodeExecution.getAdviserResponse())
                                                .build();
      waitNotifyEngine.doneWith(nodeExecution.getNotifyId(), responseData);
    } else {
      log.info("Ending Execution");
      orchestrationEngine.endNodeExecution(AmbianceUtils.cloneForFinish(ambiance));
    }
  }

  @Override
  public void handleError(Ambiance ambiance, Exception exception) {}
}
