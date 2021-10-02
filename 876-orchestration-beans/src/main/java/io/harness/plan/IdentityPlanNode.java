package io.harness.plan;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.data.stepparameters.PmsStepParameters;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
@FieldNameConstants(innerTypeName = "IdentityPlanNodeKeys")
public class IdentityPlanNode implements Node {
  @NotNull String uuid;
  @NotNull String name;
  @NotNull String identifier;
  String group;

  String originalNodeExecutionId;

  @Override
  public NodeType getNodeType() {
    return NodeType.IDENTITY_PLAN_NODE;
  }

  @Override
  public StepType getStepType() {
    return StepType.newBuilder().setType("PMS_IDENTITY").build();
  }

  @Override
  public String getServiceName() {
    return ModuleType.PMS.name();
  }

  @Override
  public PmsStepParameters getStepParameters() {
    PmsStepParameters stepParameters = new PmsStepParameters();
    stepParameters.put(IdentityPlanNodeKeys.originalNodeExecutionId, originalNodeExecutionId);
    return stepParameters;
  }
}
