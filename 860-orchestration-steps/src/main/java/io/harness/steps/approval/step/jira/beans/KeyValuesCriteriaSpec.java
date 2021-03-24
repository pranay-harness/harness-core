package io.harness.steps.approval.step.jira.beans;

import io.harness.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
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
@JsonTypeName("KeyValuesCriteriaSpec")
@TypeAlias("keyValuesCriteriaSpec")
public class KeyValuesCriteriaSpec implements CriteriaSpec {
  @NotNull
  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH)
  private ParameterField<Boolean> matchAnyCondition;
  @NotNull private List<Condition> conditions;

  @Override
  public CriteriaSpecType getType() {
    return CriteriaSpecType.KEY_VALUES;
  }
}
