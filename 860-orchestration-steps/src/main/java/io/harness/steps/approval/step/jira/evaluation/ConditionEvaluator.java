package io.harness.steps.approval.step.jira.evaluation;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.jira.JiraIssueUtils;
import io.harness.steps.approval.step.jira.beans.Operator;

import com.google.common.annotations.VisibleForTesting;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class ConditionEvaluator {
  static Map<Operator, OperatorEvaluator> evaluatorMap = new HashMap<>();
  static {
    evaluatorMap.put(Operator.EQ, new EqualsOperatorEvaluator());
    evaluatorMap.put(Operator.NOT_EQ, new NegateOperatorEvaluator(new EqualsOperatorEvaluator()));
    evaluatorMap.put(Operator.IN, new InOperatorEvaluator());
    evaluatorMap.put(Operator.NOT_IN, new NegateOperatorEvaluator(new InOperatorEvaluator()));
  }

  public boolean evaluate(Object input, String standard, Operator operator) {
    // Only string and option type fields are supported right now.
    if (operator == null) {
      throw new InvalidArgumentsException("Operator is null");
    }

    OperatorEvaluator operatorEvaluator = evaluatorMap.get(operator);
    if (operatorEvaluator == null) {
      throw new InvalidArgumentsException(
          String.format("Operator [%s] is not supported. Please use a valid one", operator));
    }

    return operatorEvaluator.evaluate(input, standard);
  }

  @VisibleForTesting
  public static class EqualsOperatorEvaluator implements OperatorEvaluator {
    @Override
    public boolean evaluate(Object input, String standard) {
      checkInputType(input);
      if (isBlank(standard)) {
        return isBlank((String) input);
      }
      return standard.equals(input);
    }
  }

  @VisibleForTesting
  public static class InOperatorEvaluator implements OperatorEvaluator {
    @Override
    public boolean evaluate(Object input, String standard) {
      checkInputType(input);
      List<String> values = JiraIssueUtils.splitByComma(standard);
      return values.contains(input);
    }
  }

  @VisibleForTesting
  public static class NegateOperatorEvaluator implements OperatorEvaluator {
    private final OperatorEvaluator evaluator;

    public NegateOperatorEvaluator(OperatorEvaluator evaluator) {
      this.evaluator = evaluator;
    }

    @Override
    public boolean evaluate(Object input, String standard) {
      return !evaluator.evaluate(input, standard);
    }
  }

  private static void checkInputType(Object input) {
    if (!(input instanceof String)) {
      throw new InvalidRequestException("Only string and option type jira fields are supported");
    }
  }
}
