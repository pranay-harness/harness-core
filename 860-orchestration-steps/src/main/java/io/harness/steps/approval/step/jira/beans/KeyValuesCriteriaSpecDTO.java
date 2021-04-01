package io.harness.steps.approval.step.jira.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import io.swagger.annotations.ApiModel;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDC)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("keyValueCriteriaSpec")
public class KeyValuesCriteriaSpecDTO implements CriteriaSpecDTO {
  boolean matchAnyCondition;
  @NotNull List<ConditionDTO> conditions;

  public static KeyValuesCriteriaSpecDTO fromKeyValueCriteria(KeyValuesCriteriaSpec keyValuesCriteriaSpec) {
    boolean matchCondition = false;
    Object matchConditionValue = keyValuesCriteriaSpec.getMatchAnyCondition().fetchFinalValue();
    if (matchConditionValue != null) {
      matchCondition = (boolean) matchConditionValue;
    }

    List<Condition> conditions = keyValuesCriteriaSpec.getConditions();
    if (isEmpty(conditions)) {
      throw new InvalidRequestException("At least 1 condition is required in KeyValues criteria");
    }

    List<ConditionDTO> conditionDTOS = new ArrayList<>();
    for (Condition condition : conditions) {
      conditionDTOS.add(ConditionDTO.fromCondition(condition));
    }
    return KeyValuesCriteriaSpecDTO.builder().matchAnyCondition(matchCondition).conditions(conditionDTOS).build();
  }
}
