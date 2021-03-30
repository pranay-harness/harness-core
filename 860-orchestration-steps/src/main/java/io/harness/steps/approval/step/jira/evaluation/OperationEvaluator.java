package io.harness.steps.approval.step.jira.evaluation;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface OperationEvaluator {
  boolean evaluate(String op1, String op2);
}
