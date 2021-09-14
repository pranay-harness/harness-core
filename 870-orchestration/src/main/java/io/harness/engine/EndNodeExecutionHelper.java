/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.springdata.SpringDataMongoUtils.setUnset;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.data.StepOutcomeRef;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.io.StepOutcomeProto;
import io.harness.pms.contracts.steps.io.StepResponseProto;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class EndNodeExecutionHelper {
  @Inject private PmsOutcomeService pmsOutcomeService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEngine orchestrationEngine;

  public void endNodeExecutionWithNoAdvisers(
      @NonNull NodeExecution nodeExecution, @NonNull StepResponseProto stepResponse) {
    NodeExecution updatedNodeExecution = processStepResponseWithNoAdvisers(nodeExecution, stepResponse);
    if (updatedNodeExecution == null) {
      log.warn("Cannot process step response for nodeExecution {}", nodeExecution.getUuid());
      return;
    }
    orchestrationEngine.endTransition(updatedNodeExecution);
  }

  private NodeExecution processStepResponseWithNoAdvisers(NodeExecution nodeExecution, StepResponseProto stepResponse) {
    // Start a transaction here
    List<StepOutcomeRef> outcomeRefs = handleOutcomes(
        nodeExecution.getAmbiance(), stepResponse.getStepOutcomesList(), stepResponse.getGraphOutcomesList());

    // End transaction here
    return nodeExecutionService.updateStatusWithOps(nodeExecution.getUuid(), stepResponse.getStatus(), ops -> {
      setUnset(ops, NodeExecutionKeys.failureInfo, stepResponse.getFailureInfo());
      setUnset(ops, NodeExecutionKeys.outcomeRefs, outcomeRefs);
      setUnset(ops, NodeExecutionKeys.unitProgresses, stepResponse.getUnitProgressList());
    }, EnumSet.noneOf(Status.class));
  }

  private List<StepOutcomeRef> handleOutcomes(
      Ambiance ambiance, List<StepOutcomeProto> stepOutcomeProtos, List<StepOutcomeProto> graphOutcomesList) {
    List<StepOutcomeRef> outcomeRefs = new ArrayList<>();
    if (isEmpty(stepOutcomeProtos)) {
      return outcomeRefs;
    }

    stepOutcomeProtos.forEach(proto -> {
      if (isNotEmpty(proto.getOutcome())) {
        String instanceId = pmsOutcomeService.consume(ambiance, proto.getName(), proto.getOutcome(), proto.getGroup());
        outcomeRefs.add(StepOutcomeRef.newBuilder().setName(proto.getName()).setInstanceId(instanceId).build());
      }
    });
    graphOutcomesList.forEach(proto -> {
      if (isNotEmpty(proto.getOutcome())) {
        String instanceId = pmsOutcomeService.consume(ambiance, proto.getName(), proto.getOutcome(), proto.getGroup());
        outcomeRefs.add(StepOutcomeRef.newBuilder().setName(proto.getName()).setInstanceId(instanceId).build());
      }
    });
    return outcomeRefs;
  }

  public NodeExecution handleStepResponsePreAdviser(NodeExecution nodeExecution, StepResponseProto stepResponse) {
    log.info("Handling Step response before calling advisers");
    return processStepResponsePreAdvisers(nodeExecution, stepResponse);
  }

  private NodeExecution processStepResponsePreAdvisers(NodeExecution nodeExecution, StepResponseProto stepResponse) {
    List<StepOutcomeRef> outcomeRefs = handleOutcomes(
        nodeExecution.getAmbiance(), stepResponse.getStepOutcomesList(), stepResponse.getGraphOutcomesList());

    log.info(
        "Trying to update nodeExecution status from {} to {}", nodeExecution.getStatus(), stepResponse.getStatus());
    return nodeExecutionService.updateStatusWithOps(nodeExecution.getUuid(), stepResponse.getStatus(), ops -> {
      setUnset(ops, NodeExecutionKeys.failureInfo, stepResponse.getFailureInfo());
      setUnset(ops, NodeExecutionKeys.outcomeRefs, outcomeRefs);
      setUnset(ops, NodeExecutionKeys.unitProgresses, stepResponse.getUnitProgressList());
    }, EnumSet.noneOf(Status.class));
  }

  public void endNodeForNullAdvise(NodeExecution nodeExecution) {
    orchestrationEngine.endTransition(nodeExecution);
  }
}
