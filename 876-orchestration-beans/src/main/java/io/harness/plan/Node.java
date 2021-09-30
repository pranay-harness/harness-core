package io.harness.plan;

import io.harness.persistence.UuidAccess;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.data.stepparameters.PmsStepParameters;

public interface Node extends UuidAccess {
  NodeType getNodeType();

  String getIdentifier();

  String getName();

  StepType getStepType();

  String getGroup();

  default boolean isSkipExpressionChain() {
    return false;
  }

  String getServiceName();

  PmsStepParameters getStepParameters();

  default String getWhenCondition() {
    return null;
  }

  default String getSkipCondition() {
    return null;
  }

  default SkipType getSkipGraphType() {
    return SkipType.NOOP;
  }
}
