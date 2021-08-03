package io.harness.steps.approval.step.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ParameterField;
import io.harness.beans.SwaggerConstants;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@Data
@Builder
public class ApproverInputInfo {
  @NotEmpty String name;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> defaultValue;
}
