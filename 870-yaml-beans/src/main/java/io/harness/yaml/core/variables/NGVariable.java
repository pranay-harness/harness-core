package io.harness.yaml.core.variables;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModelProperty;

@JsonTypeInfo(use = NAME, property = "type", include = PROPERTY, visible = true)
public interface NGVariable extends Visitable {
  NGVariableType getType();
  String getName();
  String getDescription();
  boolean isRequired();
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<?> getValue();
}
