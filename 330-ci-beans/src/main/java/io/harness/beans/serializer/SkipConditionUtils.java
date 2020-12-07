package io.harness.beans.serializer;

import io.harness.yaml.core.StepElement;

public class SkipConditionUtils {
  public static String getSkipCondition(StepElement step) {
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
