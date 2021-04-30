package io.harness.yaml.core.variables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.visitor.helpers.variables.StringVariableVisitorHelper;
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
@JsonTypeName(NGVariableConstants.STRING_TYPE)
@SimpleVisitorHelper(helperClass = StringVariableVisitorHelper.class)
@TypeAlias("io.harness.yaml.core.variables.StringNGVariable")
@OwnedBy(CDC)
public class StringNGVariable implements NGVariable {
  String name;
  @ApiModelProperty(allowableValues = NGVariableConstants.STRING_TYPE) NGVariableType type = NGVariableType.STRING;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> value;
  String description;
  boolean required;
  @JsonProperty("default") String defaultValue;

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
