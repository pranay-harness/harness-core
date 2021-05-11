package io.harness.yaml.core.variables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.encryption.SecretRefData;
import io.harness.pms.yaml.ParameterField;
import io.harness.validator.NGVariableName;
import io.harness.visitor.helpers.variables.SecretVariableVisitorHelper;
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
@JsonTypeName(NGVariableConstants.SECRET_TYPE)
@SimpleVisitorHelper(helperClass = SecretVariableVisitorHelper.class)
@TypeAlias("io.harness.yaml.core.variables.SecretNGVariable")
@OwnedBy(CDC)
public class SecretNGVariable implements NGVariable {
  @NGVariableName String name;
  @ApiModelProperty(allowableValues = NGVariableConstants.SECRET_TYPE) NGVariableType type = NGVariableType.SECRET;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<SecretRefData> value;

  String description;
  boolean required;
  @JsonProperty("default") String defaultValue;
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Override
  public ParameterField<?> getCurrentValue() {
    return value;
  }
}