package io.harness.steps.approval.step.jira.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.yaml.ParameterField;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@Data
@Builder
public class ConditionDTO {
  @NotEmpty String key;
  @NotNull String value;
  @NotNull Operator op;

  public static ConditionDTO fromCondition(Condition condition) {
    if (condition == null) {
      return null;
    }
    if (ParameterField.isNull(condition.getValue())) {
      throw new InvalidRequestException("Value can't be null");
    }

    String valueString = (String) condition.getValue().fetchFinalValue();
    if (isBlank(condition.getKey())) {
      throw new InvalidRequestException("Key Can't be empty");
    }
    return ConditionDTO.builder().key(condition.getKey()).value(valueString).op(condition.getOp()).build();
  }

  @Override
  public String toString() {
    return "ConditionDTO{"
        + "key='" + key + '\'' + ", value='" + value + '\'' + ", op=" + op + '}';
  }
}
