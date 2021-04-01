package io.harness.steps.approval.step;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotations.dev.OwnedBy;
import io.harness.common.SwaggerConstants;
import io.harness.plancreator.steps.internal.PMSStepInfo;
import io.harness.pms.sdk.core.steps.io.WithRollbackInfo;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@TypeAlias("approvalBaseStepInfo")
public abstract class ApprovalBaseStepInfo implements PMSStepInfo, WithRollbackInfo {
  @JsonIgnore String name;
  @JsonIgnore String identifier;

  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> approvalMessage;

  @NotNull
  @YamlSchemaTypes(value = {string})
  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH)
  ParameterField<Boolean> includePipelineExecutionHistory;
}
