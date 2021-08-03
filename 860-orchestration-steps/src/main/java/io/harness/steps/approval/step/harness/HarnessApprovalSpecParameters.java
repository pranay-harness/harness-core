package io.harness.steps.approval.step.harness;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ParameterField;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.steps.approval.step.harness.beans.ApproverInputInfo;
import io.harness.steps.approval.step.harness.beans.Approvers;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("harnessApprovalSpecParameters")
public class HarnessApprovalSpecParameters implements SpecParameters {
  @NotNull ParameterField<String> approvalMessage;
  @NotNull ParameterField<Boolean> includePipelineExecutionHistory;

  @NotNull Approvers approvers;
  List<ApproverInputInfo> approverInputs;
}
