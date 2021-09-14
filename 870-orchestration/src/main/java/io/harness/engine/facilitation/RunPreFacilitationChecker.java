/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.facilitation;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.ExecutionCheck;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.expressions.OrchestrationConstants;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.run.ExpressionBlock;
import io.harness.pms.contracts.execution.run.NodeRunInfo;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.expression.PmsEngineExpressionService;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
public class RunPreFacilitationChecker extends ExpressionEvalPreFacilitationChecker {
  @Inject private OrchestrationEngine orchestrationEngine;
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject PmsEngineExpressionService pmsEngineExpressionService;

  @Override
  protected ExecutionCheck performCheck(NodeExecution nodeExecution) {
    log.info("Checking If Node should be Run with When Condition.");
    Ambiance ambiance = nodeExecution.getAmbiance();
    String whenCondition = nodeExecution.getNode().getWhenCondition();
    if (EmptyPredicate.isNotEmpty(whenCondition)) {
      try {
        EngineExpressionEvaluator engineExpressionEvaluator =
            pmsEngineExpressionService.prepareExpressionEvaluator(ambiance);
        Object evaluatedExpression = engineExpressionEvaluator.evaluateExpression(whenCondition);
        boolean whenConditionValue = (Boolean) evaluatedExpression;
        nodeExecutionService.update(nodeExecution.getUuid(), ops -> {
          ops.set(NodeExecutionKeys.nodeRunInfo,
              NodeRunInfo.newBuilder()
                  .setEvaluatedCondition(whenConditionValue)
                  .setWhenCondition(whenCondition)
                  .addAllExpressions(getAllExpressions(engineExpressionEvaluator))
                  .build());
        });
        if (!whenConditionValue) {
          log.info(String.format("Skipping node: %s", nodeExecution.getUuid()));
          StepResponseProto response =
              StepResponseProto.newBuilder()
                  .setStatus(Status.SKIPPED)
                  .setNodeRunInfo(
                      NodeRunInfo.newBuilder().setWhenCondition(whenCondition).setEvaluatedCondition(false).build())
                  .build();
          orchestrationEngine.handleStepResponse(nodeExecution.getUuid(), response);
          return ExecutionCheck.builder().proceed(false).reason("When Condition Evaluated to false").build();
        }
        return ExecutionCheck.builder().proceed(true).reason("When Condition Evaluated to true").build();
      } catch (Exception ex) {
        return handleExpressionEvaluationError(ex, whenCondition, ambiance);
      }
    }
    return ExecutionCheck.builder().proceed(true).reason("No when Condition Configured").build();
  }

  @VisibleForTesting
  List<ExpressionBlock> getAllExpressions(EngineExpressionEvaluator engineExpressionEvaluator) {
    Map<String, Map<Object, Integer>> usedExpressionsMap =
        engineExpressionEvaluator.getVariableResolverTracker().getUsage();
    List<ExpressionBlock> resultExpressionsList = new LinkedList<>();
    for (Map.Entry<String, Map<Object, Integer>> stringMapEntry : usedExpressionsMap.entrySet()) {
      String expression = stringMapEntry.getKey();
      // Removing one internal expression.
      if (expression.contains(OrchestrationConstants.CURRENT_STATUS)) {
        continue;
      }
      Set<Object> expressionValueSet = stringMapEntry.getValue().keySet();
      for (Object value : expressionValueSet) {
        String expressionValue = String.valueOf(value);
        ExpressionBlock expressionBlock = ExpressionBlock.newBuilder()
                                              .setExpression(expression)
                                              .setExpressionValue(expressionValue)
                                              .setCount(stringMapEntry.getValue().get(value))
                                              .build();
        resultExpressionsList.add(expressionBlock);
      }
    }
    return resultExpressionsList;
  }
}
