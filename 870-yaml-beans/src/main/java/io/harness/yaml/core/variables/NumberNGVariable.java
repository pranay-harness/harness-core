package io.harness.yaml.core.variables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.validator.NGVariableName;
import io.harness.visitor.helpers.variables.NumberVariableVisitorHelper;
import io.harness.walktree.visitor.SimpleVisitorHelper;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName(NGVariableConstants.NUMBER_TYPE)
@SimpleVisitorHelper(helperClass = NumberVariableVisitorHelper.class)
@TypeAlias("io.harness.yaml.core.variables.NumberNGVariable")
@OwnedBy(CDC)
public class NumberNGVariable implements NGVariable {
  @NGVariableName String name;
  @ApiModelProperty(allowableValues = NGVariableConstants.NUMBER_TYPE) NGVariableType type = NGVariableType.NUMBER;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.DOUBLE_CLASSPATH) ParameterField<Double> value;
  String description;
  boolean required;
  @JsonProperty("default") Double defaultValue;
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Override
  public ParameterField<?> getCurrentValue() {
    return value;
  }
}
