package io.harness.yaml.core.variables;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModelProperty;

@JsonTypeInfo(use = NAME, property = "type", include = PROPERTY, visible = true)
@JsonSubTypes({
  @Type(value = NumberNGVariable.class, name = NGVariableConstants.NUMBER_TYPE)
  , @Type(value = StringNGVariable.class, name = NGVariableConstants.STRING_TYPE),
      @Type(value = SecretNGVariable.class, name = NGVariableConstants.SECRET_TYPE)
})
public interface NGVariable extends Visitable {
  NGVariableType getType();
  String getName();
  String getDescription();
  boolean isRequired();
  @ApiModelProperty(hidden = true) ParameterField<?> getCurrentValue();
}
