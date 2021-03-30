package io.harness.steps.approval.step.jira.evaluation;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.jira.JiraIssueNG;
import io.harness.steps.approval.step.jira.JiraExpressionEvaluator;
import io.harness.steps.approval.step.jira.beans.ConditionDTO;
import io.harness.steps.approval.step.jira.beans.CriteriaSpecDTO;
import io.harness.steps.approval.step.jira.beans.JexlCriteriaSpecDTO;
import io.harness.steps.approval.step.jira.beans.KeyValuesCriteriaSpecDTO;
import io.harness.steps.approval.step.jira.beans.Operator;

import java.util.List;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class ConditionEvaluationHelper {
  public boolean evaluateCondition(JiraIssueNG issue, CriteriaSpecDTO criteriaSpec) {
    if (criteriaSpec instanceof JexlCriteriaSpecDTO) {
      return evaluateJexlCondition(issue, (JexlCriteriaSpecDTO) criteriaSpec);
    } else if (criteriaSpec instanceof KeyValuesCriteriaSpecDTO) {
      return evaluateKeyValueCriteria(issue, (KeyValuesCriteriaSpecDTO) criteriaSpec);
    } else {
      throw new InvalidRequestException("Unknown Criteria Spec Type");
    }
  }

  public boolean evaluateJexlCondition(JiraIssueNG issue, JexlCriteriaSpecDTO jexlCriteriaSpec) {
    String expression = jexlCriteriaSpec.getExpression();
    try {
      JiraExpressionEvaluator jiraExpressionEvaluator = new JiraExpressionEvaluator(issue);

      Object result = jiraExpressionEvaluator.evaluateExpression(expression);
      if (result instanceof Boolean) {
        return (boolean) result;
      } else {
        throw new InvalidRequestException("Non boolean result while evaluating approval condition");
      }
    } catch (Exception e) {
      throw new InvalidRequestException(
          String.format("Error while evaluating approval condition. expression: %s%n", expression), e);
    }
  }
  public boolean evaluateKeyValueCriteria(JiraIssueNG issue, KeyValuesCriteriaSpecDTO keyValueCriteriaSpec) {
    List<ConditionDTO> conditions = keyValueCriteriaSpec.getConditions();

    if (isEmpty(conditions)) {
      throw new InvalidRequestException("Conditions in KeyValueCriteria can't be empty");
    }

    JiraExpressionEvaluator jiraExpressionEvaluator = new JiraExpressionEvaluator(issue);

    boolean matchAnyCondition = keyValueCriteriaSpec.isMatchAnyCondition();
    boolean finalResult = !matchAnyCondition; // Initial value false in case of OR, true in case of AND.

    for (ConditionDTO condition : conditions) {
      try {
        String key = (String) jiraExpressionEvaluator.evaluateExpression(condition.getKey());
        String value = condition.getValue();
        Operator op = condition.getOp();
        boolean currentResult = ConditionEvaluator.evaluate(key, value, op);
        if (matchAnyCondition) {
          finalResult = finalResult || currentResult;
          if (finalResult) { // Break early in case evaluating Or
            return true;
          }
        } else {
          finalResult = finalResult && currentResult;
          if (!finalResult) { // Break early in case evaluating And
            return false;
          }
        }
      } catch (Exception e) {
        throw new InvalidRequestException(
            String.format("Error while evaluating condition %s%n", condition.toString()), e);
      }
    }
    return finalResult;
  }
}
