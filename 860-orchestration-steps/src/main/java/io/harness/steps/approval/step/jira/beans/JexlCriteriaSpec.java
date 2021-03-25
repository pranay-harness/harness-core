package io.harness.steps.approval.step.jira.beans;

import io.harness.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonTypeName("Jexl")
@TypeAlias("jexlCriteriaSpec")
public class JexlCriteriaSpec implements CriteriaSpec {
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) private ParameterField<String> expression;

  @Override
  public CriteriaSpecType getType() {
    return CriteriaSpecType.JEXL;
  }
}
