package io.harness.engine;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.pms.contracts.execution.Status.EXPIRED;
import static io.harness.springdata.SpringDataMongoUtils.setUnset;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.engine.utils.TransactionUtils;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.data.StepOutcomeRef;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.steps.io.StepOutcomeProto;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.utils.RetryUtils;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.transaction.TransactionException;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class EndNodeExecutionHelper {
  private final RetryPolicy<Object> transactionRetryPolicy = RetryUtils.getRetryPolicy("[Retrying] attempt: {}",
      "[Failed] attempt: {}", ImmutableList.of(TransactionException.class), Duration.ofSeconds(1), 3, log);

  @Inject private PmsOutcomeService pmsOutcomeService;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private TransactionUtils transactionUtils;
  @Inject private OrchestrationEngine orchestrationEngine;

  public void endNodeExecutionWithNoAdvisers(
      @NonNull NodeExecution nodeExecution, @NonNull StepResponseProto stepResponse) {
    NodeExecution updatedNodeExecution =
        transactionUtils.performTransaction(() -> processStepResponseWithNoAdvisers(nodeExecution, stepResponse));
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
      if (stepResponse.getStatus() != EXPIRED) {
        setUnset(ops, NodeExecutionKeys.timeoutInstanceIds, new ArrayList<>());
      }
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
    return transactionUtils.performTransaction(() -> processStepResponsePreAdvisers(nodeExecution, stepResponse));
  }

  private NodeExecution processStepResponsePreAdvisers(NodeExecution nodeExecution, StepResponseProto stepResponse) {
    List<StepOutcomeRef> outcomeRefs = handleOutcomes(
        nodeExecution.getAmbiance(), stepResponse.getStepOutcomesList(), stepResponse.getGraphOutcomesList());

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
