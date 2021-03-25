package io.harness.steps.approval.step.jira.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.yaml.ParameterField;

import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("JexlCriteria")
public class JexlCriteriaSpecDTO implements CriteriaSpecDTO {
  @NotEmpty String expression;

  public static JexlCriteriaSpecDTO fromJexlCriteriaSpec(JexlCriteriaSpec jexlCriteriaSpec) {
    if (ParameterField.isNull(jexlCriteriaSpec.getExpression())) {
      throw new InvalidRequestException("Expression can't be null");
    }
    String expressionString = (String) jexlCriteriaSpec.getExpression().fetchFinalValue();
    return JexlCriteriaSpecDTO.builder().expression(expressionString).build();
  }
}
