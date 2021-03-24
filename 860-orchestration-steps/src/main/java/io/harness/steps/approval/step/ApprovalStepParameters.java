package io.harness.steps.approval.step;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.common.SwaggerConstants;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.steps.approval.step.beans.ApprovalType;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("approvalStepParameters")
public abstract class ApprovalStepParameters implements StepParameters {
  String name;
  String identifier;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> timeout;

  ApprovalType approvalType;

  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> approvalMessage;
  @NotNull
  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH)
  ParameterField<Boolean> includePipelineExecutionHistory;
}
