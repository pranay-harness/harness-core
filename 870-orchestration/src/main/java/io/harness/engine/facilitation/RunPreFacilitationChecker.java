package io.harness.engine.facilitation;

import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.interrupts.PreFacilitationCheck;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.run.NodeRunInfo;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.expression.EngineExpressionService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RunPreFacilitationChecker extends ExpressionEvalPreFacilitationChecker {
  @Inject private EngineExpressionService engineExpressionService;
  @Inject private OrchestrationEngine orchestrationEngine;
  @Inject private NodeExecutionService nodeExecutionService;

  @Override
  protected PreFacilitationCheck performCheck(NodeExecution nodeExecution) {
    log.info("Checking If Node should be Run with When Condition.");
    Ambiance ambiance = nodeExecution.getAmbiance();
    String whenCondition = nodeExecution.getNode().getWhenCondition();
    if (EmptyPredicate.isNotEmpty(whenCondition)) {
      try {
        boolean whenConditionValue = (Boolean) engineExpressionService.evaluateExpression(ambiance, whenCondition);
        nodeExecutionService.update(nodeExecution.getUuid(), ops -> {
          ops.set(NodeExecutionKeys.nodeRunInfo,
              NodeRunInfo.newBuilder()
                  .setEvaluatedCondition(whenConditionValue)
                  .setWhenCondition(whenCondition)
                  .build());
        });
        if (!whenConditionValue) {
          log.info(String.format("Skipping node: %s", nodeExecution.getUuid()));
          StepResponseProto response = StepResponseProto.newBuilder()
                                           .setStatus(Status.SKIPPED)
                                           .setNodeRunInfo(NodeRunInfo.newBuilder()
                                                               .setWhenCondition(whenCondition)
                                                               .setEvaluatedCondition(whenConditionValue)
                                                               .build())
                                           .build();
          orchestrationEngine.handleStepResponse(nodeExecution.getUuid(), response);
          return PreFacilitationCheck.builder().proceed(false).reason("When Condition Evaluated to true").build();
        }
        return PreFacilitationCheck.builder().proceed(true).reason("When Condition Evaluated to false").build();
      } catch (Exception ex) {
        return handleExpressionEvaluationError(nodeExecution.getUuid(), ex);
      }
    }
    return PreFacilitationCheck.builder().proceed(true).reason("No when Condition Configured").build();
  }
}
