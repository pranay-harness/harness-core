package io.harness.yaml.core.variables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.common.NGExpressionUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.validator.NGRegexValidatorConstants;
import io.harness.validator.NGVariableName;
import io.harness.visitor.helpers.variables.StringVariableVisitorHelper;
import io.harness.walktree.visitor.SimpleVisitorHelper;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
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
  @NGVariableName @Pattern(regexp = NGRegexValidatorConstants.IDENTIFIER_PATTERN) String name;
  @ApiModelProperty(allowableValues = NGVariableConstants.STRING_TYPE) NGVariableType type = NGVariableType.STRING;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> value;
  String description;
  boolean required;
  @JsonProperty("default") String defaultValue;

  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Override
  public ParameterField<?> getCurrentValue() {
    return ParameterField.isNull(value)
            || (value.isExpression() && NGExpressionUtils.matchesInputSetPattern(value.getExpressionValue()))
        ? ParameterField.createValueField(defaultValue)
        : value;
  }
}
