package io.harness.steps.approval.step.jira.evaluation;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER_SRE;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArgumentsException;
import io.harness.steps.approval.step.jira.beans.Operator;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class ConditionEvaluator {
  static Map<Operator, OperationEvaluator> evaluatorMap = new HashMap<>();
  static {
    evaluatorMap.put(Operator.EQ, new EqualsOperatorEvaluator());
    evaluatorMap.put(Operator.NOT_EQ, new NotEqualsOperatorEvaluator());
  }

  public boolean evaluate(String input, String standard, Operator operator) {
    if (isBlank(input) && isBlank(standard)) {
      return true;
    }
    OperationEvaluator operationEvaluator = evaluatorMap.get(operator);
    if (operationEvaluator == null) {
      throw new InvalidArgumentsException(
          String.format("Operator [%s] is not supported. Please user valid one", operator), USER_SRE);
    }

    return operationEvaluator.evaluate(input, standard);
  }

  static class EqualsOperatorEvaluator implements OperationEvaluator {
    @Override
    public boolean evaluate(String input, String standard) {
      if (isBlank(standard)) {
        return isBlank(input);
      }

      return standard.equals(input);
    }
  }

  static class NotEqualsOperatorEvaluator implements OperationEvaluator {
    @Override
    public boolean evaluate(String input, String standard) {
      // standard is missing, evaluation cant happen
      if (isBlank(standard)) {
        return isNotBlank(input);
      }

      return !standard.equals(input);
    }
  }
}
