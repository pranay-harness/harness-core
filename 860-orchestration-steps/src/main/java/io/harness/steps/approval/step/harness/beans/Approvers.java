package io.harness.steps.approval.step.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.runtime;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDC)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@RecasterAlias("io.harness.steps.approval.step.harness.beans.Approvers")
public class Approvers {
  @NotNull
  @YamlSchemaTypes(value = {runtime})
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  ParameterField<List<String>> userGroups;

  @NotNull
  @YamlSchemaTypes(value = {string})
  @ApiModelProperty(dataType = SwaggerConstants.INTEGER_CLASSPATH)
  ParameterField<Integer> minimumCount;

  @NotNull
  @YamlSchemaTypes(value = {string})
  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH)
  ParameterField<Boolean> disallowPipelineExecutor;
}
