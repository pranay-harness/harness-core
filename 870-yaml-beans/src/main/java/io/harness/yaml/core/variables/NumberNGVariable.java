package io.harness.yaml.core.variables;

import io.harness.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.visitor.helpers.variables.NumberVariableVisitorHelper;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.yaml.core.LevelNodeQualifierName;

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
public class NumberNGVariable implements NGVariable {
  String name;
  @ApiModelProperty(allowableValues = NGVariableConstants.NUMBER_TYPE) NGVariableType type = NGVariableType.NUMBER;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.DOUBLE_CLASSPATH) ParameterField<Double> value;
  String description;
  boolean required;
  @JsonProperty("default") Double defaultValue;
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder()
        .qualifierName(LevelNodeQualifierName.NG_VARIABLES + LevelNodeQualifierName.PATH_CONNECTOR + name)
        .build();
  }

  @Override
  public ParameterField<?> getCurrentValue() {
    return value;
  }
}
