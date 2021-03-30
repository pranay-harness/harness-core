package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.pms.contracts.steps.StepType;

@OwnedBy(CDP)
public class TerraformApplyStep {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.TF_APPLY.getYamlType()).build();
}
