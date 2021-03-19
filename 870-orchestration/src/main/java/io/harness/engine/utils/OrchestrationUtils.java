package io.harness.engine.utils;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.run.NodeRunCheck;
import io.harness.engine.skip.SkipCheck;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.expression.EngineExpressionService;

import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class OrchestrationUtils {
  public Status calculateEndStatus(List<NodeExecution> nodeExecutions, String planExecutionId) {
    List<Status> statuses = nodeExecutions.stream()
                                .map(NodeExecution::getStatus)
                                .filter(s -> !StatusUtils.finalizableStatuses().contains(s))
                                .collect(Collectors.toList());
    return StatusUtils.calculateEndStatus(statuses, planExecutionId);
  }

  public NodeRunCheck shouldRunExecution(
      Ambiance ambiance, String whenCondition, EngineExpressionService engineExpressionService) {
    if (EmptyPredicate.isEmpty(whenCondition)) {
      return NodeRunCheck.builder().isSuccessful(false).whenCondition(whenCondition).build();
    }
    try {
      String evaluatedExpression = (String) engineExpressionService.evaluateExpression(ambiance, whenCondition);
      boolean whenConditionValue = Boolean.parseBoolean(evaluatedExpression);
      return NodeRunCheck.builder()
          .whenCondition(whenCondition)
          .isSuccessful(true)
          .evaluatedWhenCondition(whenConditionValue)
          .build();
    } catch (Exception exception) {
      return NodeRunCheck.builder()
          .whenCondition(whenCondition)
          .isSuccessful(false)
          .errorMessage(String.format(
              "The when condition could not be evaluated because an expression [%s] is formatted incorrectly and the condition cannot be resolved true (skip) or false (do not skip).",
              whenCondition))
          .build();
    }
  }

  public SkipCheck shouldSkipNodeExecution(
      Ambiance ambiance, String skipCondition, EngineExpressionService engineExpressionService) {
    if (EmptyPredicate.isEmpty(skipCondition)) {
      return SkipCheck.builder().isSuccessful(false).skipCondition(skipCondition).build();
    }
    try {
      String evaluatedExpression = (String) engineExpressionService.evaluateExpression(ambiance, skipCondition);
      boolean skipConditionValue = Boolean.parseBoolean(evaluatedExpression);
      return SkipCheck.builder()
          .skipCondition(skipCondition)
          .isSuccessful(true)
          .evaluatedSkipCondition(skipConditionValue)
          .build();
    } catch (Exception exception) {
      return SkipCheck.builder()
          .skipCondition(skipCondition)
          .isSuccessful(false)
          .errorMessage(String.format(
              "The skip condition could not be evaluated because an expression [%s] is formatted incorrectly and the condition cannot be resolved true (skip) or false (do not skip).",
              skipCondition))
          .build();
    }
  }
}
