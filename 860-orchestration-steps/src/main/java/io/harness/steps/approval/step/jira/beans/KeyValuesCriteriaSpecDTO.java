/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.steps.approval.step.jira.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;

import io.swagger.annotations.ApiModel;
import java.util.ArrayList;
import java.util.Collections;
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

  @Override
  public boolean isEmpty() {
    return EmptyPredicate.isEmpty(conditions);
  }

  public static KeyValuesCriteriaSpecDTO fromKeyValueCriteria(
      KeyValuesCriteriaSpec keyValuesCriteriaSpec, boolean skipEmpty) {
    boolean matchCondition = false;
    Object matchConditionValue = keyValuesCriteriaSpec.getMatchAnyCondition().fetchFinalValue();
    if (matchConditionValue != null) {
      matchCondition = (boolean) matchConditionValue;
    }

    List<Condition> conditions = keyValuesCriteriaSpec.getConditions();
    if (EmptyPredicate.isEmpty(conditions)) {
      if (skipEmpty) {
        return KeyValuesCriteriaSpecDTO.builder()
            .matchAnyCondition(matchCondition)
            .conditions(Collections.emptyList())
            .build();
      }
      throw new InvalidRequestException("At least 1 condition is required in KeyValues criteria");
    }

    List<ConditionDTO> conditionDTOS = new ArrayList<>();
    for (Condition condition : conditions) {
      conditionDTOS.add(ConditionDTO.fromCondition(condition));
    }
    return KeyValuesCriteriaSpecDTO.builder().matchAnyCondition(matchCondition).conditions(conditionDTOS).build();
  }
}
