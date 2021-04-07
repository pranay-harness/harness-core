package io.harness.steps.approval.step;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.approval.step.beans.ApprovalType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("approvalStepParameters")
public abstract class ApprovalStepParameters implements StepParameters {
  String name;
  String identifier;
  ParameterField<String> timeout;

  ApprovalType approvalType;
}
