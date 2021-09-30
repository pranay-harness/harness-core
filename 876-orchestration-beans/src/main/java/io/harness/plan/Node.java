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

  boolean isSkipExpressionChain();

  String getServiceName();

  PmsStepParameters getStepParameters();

  String getWhenCondition();

  String getSkipCondition();

  SkipType getSkipGraphType();
}
