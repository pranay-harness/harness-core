package io.harness.steps.approval.step.jira.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class Condition {
  @NotNull String key;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> value;
  @NotNull Operator operator;
}
