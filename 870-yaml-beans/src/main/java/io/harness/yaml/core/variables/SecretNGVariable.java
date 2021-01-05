package io.harness.yaml.core.variables;

import io.harness.common.SwaggerConstants;
import io.harness.encryption.SecretRefData;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.beans.LevelNode;
import io.harness.yaml.core.LevelNodeQualifierName;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.sun.istack.internal.NotNull;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName(NGVariableConstants.SECRET_TYPE)
@TypeAlias("io.harness.yaml.core.variables.SecretNGVariable")
public class SecretNGVariable implements NGVariable {
  String name;
  @Builder.Default NGVariableType type = NGVariableType.SECRET;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<SecretRefData> value;

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