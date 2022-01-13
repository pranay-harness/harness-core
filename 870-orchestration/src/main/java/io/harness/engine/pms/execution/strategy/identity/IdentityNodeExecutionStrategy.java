/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.pms.execution.strategy.identity;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.springdata.SpringDataMongoUtils.setUnset;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.pms.advise.AdviseHandlerFactory;
import io.harness.engine.pms.advise.AdviserResponseHandler;
import io.harness.engine.pms.commons.events.PmsEventSender;
import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.engine.pms.data.PmsSweepingOutputService;
import io.harness.engine.pms.execution.strategy.NodeExecutionStrategy;
import io.harness.engine.utils.PmsLevelUtils;
import io.harness.execution.ExecutionModeUtils;
import io.harness.execution.IdentityNodeExecutionMetadata;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.logging.AutoLogContext;
import io.harness.plan.IdentityPlanNode;
import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.start.NodeStartEvent;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.events.base.PmsEventCategory;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.execution.utils.NodeProjectionUtils;
import io.harness.pms.sdk.core.steps.io.StepResponseNotifyData;
import io.harness.springdata.TransactionHelper;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class IdentityNodeExecutionStrategy
    implements NodeExecutionStrategy<IdentityPlanNode, NodeExecution, IdentityNodeExecutionMetadata> {
  @Inject private PmsEventSender eventSender;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private AdviseHandlerFactory adviseHandlerFactory;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private OrchestrationEngine orchestrationEngine;
  @Inject private PmsOutcomeService pmsOutcomeService;
  @Inject private PmsSweepingOutputService pmsSweepingOutputService;
  @Inject private IdentityNodeResumeHelper identityNodeResumeHelper;
  @Inject private TransactionHelper transactionHelper;
  @Inject @Named("EngineExecutorService") private ExecutorService executorService;

  private String SERVICE_NAME_IDENTITY = ModuleType.PMS.name().toLowerCase();

  private void setNodeExecutionParameters(Update update, NodeExecution originalExecution) {
    setUnset(update, NodeExecutionKeys.resolvedStepParameters, originalExecution.getResolvedStepParameters());
    setUnset(update, NodeExecutionKeys.resolvedInputs, originalExecution.getResolvedInputs());
    setUnset(update, NodeExecutionKeys.mode, originalExecution.getMode());
    setUnset(update, NodeExecutionKeys.nodeRunInfo, originalExecution.getNodeRunInfo());
    setUnset(update, NodeExecutionKeys.skipInfo, originalExecution.getSkipInfo());
    setUnset(update, NodeExecutionKeys.failureInfo, originalExecution.getFailureInfo());
    setUnset(update, NodeExecutionKeys.progressData, originalExecution.getProgressData());
    setUnset(update, NodeExecutionKeys.adviserResponse, originalExecution.getAdviserResponse());
    setUnset(update, NodeExecutionKeys.timeoutInstanceIds, originalExecution.getTimeoutInstanceIds());
    setUnset(update, NodeExecutionKeys.timeoutDetails, originalExecution.getTimeoutDetails());
    setUnset(update, NodeExecutionKeys.adviserTimeoutInstanceIds, originalExecution.getAdviserTimeoutInstanceIds());
    setUnset(update, NodeExecutionKeys.adviserTimeoutDetails, originalExecution.getAdviserTimeoutDetails());
    setUnset(update, NodeExecutionKeys.interruptHistories, originalExecution.getInterruptHistories());
  }

  @Override
  public NodeExecution triggerNode(Ambiance ambiance, IdentityPlanNode node, IdentityNodeExecutionMetadata metadata) {
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
    NodeExecution savedNodeExecution = nodeExecutionService.save(nodeExecution);
    // TODO: Should add to an execution queue rather than submitting straight to thread pool
    executorService.submit(() -> startExecution(cloned));
    return savedNodeExecution;
  }

  @Override
  public void startExecution(Ambiance ambiance) {
    String newNodeExecutionId = Objects.requireNonNull(AmbianceUtils.obtainCurrentRuntimeId(ambiance));
    NodeExecution newNodeExecution = nodeExecutionService.get(AmbianceUtils.obtainCurrentRuntimeId(ambiance));
    IdentityPlanNode node = newNodeExecution.getNode();
    try (AutoLogContext ignore = AmbianceUtils.autoLogContext(ambiance)) {
      NodeExecution originalExecution = nodeExecutionService.get(node.getOriginalNodeExecutionId());

      Update update = new Update();
      setNodeExecutionParameters(update, originalExecution);

      // If Node is skipped then call the adviser response handler straight away
      if (originalExecution.getStatus() == Status.SKIPPED) {
        newNodeExecution = nodeExecutionService.updateStatusWithUpdate(
            newNodeExecutionId, originalExecution.getStatus(), update, EnumSet.noneOf(Status.class));
        processAdviserResponse(ambiance, newNodeExecution.getAdviserResponse());
        return;
      }

      // If this is one of the leaf modes then just clone and copy everything and we should be good
      // This is an optimization/hack to not do any actual work
      if (ExecutionModeUtils.isLeafMode(originalExecution.getMode())) {
        handleLeafNodes(ambiance, originalExecution, update, newNodeExecutionId);
        return;
      }

      newNodeExecution = nodeExecutionService.updateStatusWithUpdate(
          newNodeExecutionId, Status.RUNNING, update, EnumSet.noneOf(Status.class));

      // If not leaf node then we need to call the identity step
      Ambiance modifyAmbiance = IdentityStep.modifyAmbiance(ambiance);
      NodeStartEvent nodeStartEvent = NodeStartEvent.newBuilder()
                                          .setAmbiance(modifyAmbiance)
                                          .setStepParameters(newNodeExecution.getResolvedStepParametersBytes())
                                          .setMode(newNodeExecution.getMode())
                                          .build();
      // hard code of service name to PMS
      eventSender.sendEvent(
          modifyAmbiance, nodeStartEvent.toByteString(), PmsEventCategory.NODE_START, SERVICE_NAME_IDENTITY, true);
    } catch (Exception exception) {
      log.error("Exception Occurred in facilitateAndStartStep NodeExecutionId : {}, PlanExecutionId: {}",
          AmbianceUtils.obtainCurrentRuntimeId(ambiance), ambiance.getPlanExecutionId(), exception);
      handleError(ambiance, exception);
    }
  }

  private void handleLeafNodes(
      Ambiance ambiance, NodeExecution originalExecution, Update update, String newNodeExecutionId) {
    NodeExecution newNodeExecution = transactionHelper.performTransaction(() -> {
      // Copy outcomes
      pmsOutcomeService.cloneForRetryExecution(ambiance, originalExecution.getUuid());
      // Copy outputs
      pmsSweepingOutputService.cloneForRetryExecution(ambiance, originalExecution.getUuid());

      return nodeExecutionService.updateStatusWithUpdate(
          newNodeExecutionId, originalExecution.getStatus(), update, EnumSet.noneOf(Status.class));
    });

    processAdviserResponse(ambiance, newNodeExecution.getAdviserResponse());
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
      IdentityPlanNode planNode = nodeExecution.getNode();
      StepResponseNotifyData responseData = StepResponseNotifyData.builder()
                                                .nodeUuid(planNode.getUuid())
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

  @Override
  public void resumeNodeExecution(Ambiance ambiance, Map<String, ByteString> response, boolean asyncError) {
    String nodeExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    NodeExecution nodeExecution =
        nodeExecutionService.getWithFieldsIncluded(nodeExecutionId, NodeProjectionUtils.fieldsForResume);
    try (AutoLogContext ignore = AmbianceUtils.autoLogContext(ambiance)) {
      identityNodeResumeHelper.resume(nodeExecution, response, asyncError, SERVICE_NAME_IDENTITY);
    } catch (Exception exception) {
      log.error("Exception Occurred in handling resume with nodeExecutionId {} planExecutionId {}", nodeExecutionId,
          ambiance.getPlanExecutionId(), exception);
      handleError(ambiance, exception);
    }
  }

  @Override
  public void processStepResponse(Ambiance ambiance, StepResponseProto stepResponse) {
    String nodeExecutionId = AmbianceUtils.obtainCurrentRuntimeId(ambiance);
    try (AutoLogContext ignore = AmbianceUtils.autoLogContext(ambiance)) {
      NodeExecution newNodeExecution = nodeExecutionService.updateStatusWithOps(
          nodeExecutionId, stepResponse.getStatus(), null, EnumSet.noneOf(Status.class));
      processAdviserResponse(ambiance, newNodeExecution.getAdviserResponse());
    } catch (Exception ex) {
      log.error("Exception Occurred in handleStepResponse NodeExecutionId : {}, PlanExecutionId: {}", nodeExecutionId,
          ambiance.getPlanExecutionId(), ex);
      handleError(ambiance, ex);
    }
  }
}
