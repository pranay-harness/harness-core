package io.harness.plan;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.UuidAccess;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.data.stepparameters.PmsStepParameters;

@OwnedBy(HarnessTeam.PIPELINE)
public interface Node extends UuidAccess {
  NodeType getNodeType();

  String getIdentifier();

  String getStageFqn();

  String getName();

  StepType getStepType();

  String getGroup();

  default boolean isSkipExpressionChain() {
    return false;
  }

  @Deprecated String getServiceName();

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

  default StepCategory getStepCategory() {
    return getStepType().getStepCategory();
  }
}
