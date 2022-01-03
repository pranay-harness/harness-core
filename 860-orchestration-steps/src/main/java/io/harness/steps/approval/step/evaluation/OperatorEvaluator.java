package io.harness.steps.approval.step.evaluation;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public interface OperatorEvaluator {
  boolean evaluate(Object input, String standard);
}
