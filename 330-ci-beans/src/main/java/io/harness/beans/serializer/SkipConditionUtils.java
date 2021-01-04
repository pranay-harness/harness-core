package io.harness.beans.serializer;

import io.harness.plancreator.steps.StepElementConfig;

public class SkipConditionUtils {
  public static String getSkipCondition(StepElementConfig step) {
    String skipCondition = null;
    if (step.getSkipCondition() != null) {
      skipCondition = step.getSkipCondition().getValue();
      if (skipCondition == null) {
        skipCondition = step.getSkipCondition().getExpressionValue();
      }
    }
    return skipCondition;
  }
}
