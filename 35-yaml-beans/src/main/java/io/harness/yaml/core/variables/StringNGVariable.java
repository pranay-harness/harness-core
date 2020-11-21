package io.harness.yaml.core.variables;

import io.harness.beans.ParameterField;
import io.harness.common.SwaggerConstants;
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
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName(NGVariableConstants.STRING_TYPE)
@SimpleVisitorHelper(helperClass = StringVariableVisitorHelper.class)
public class StringNGVariable implements NGVariable {
  String name;
  @Builder.Default NGVariableType type = NGVariableType.STRING;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> value;
  String description;
  boolean required;
  @JsonProperty("default") String defaultValue;

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder()
        .qualifierName(LevelNodeQualifierName.NG_VARIABLES + LevelNodeQualifierName.PATH_CONNECTOR + name)
        .build();
  }
}
