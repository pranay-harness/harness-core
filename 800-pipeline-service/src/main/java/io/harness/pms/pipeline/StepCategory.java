/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StepCategory {
  String name;
  @Builder.Default List<StepData> stepsData = new ArrayList<>();
  @Builder.Default List<StepCategory> stepCategories = new ArrayList<>();

  public void addStepData(StepData stepData) {
    stepsData.add(stepData);
  }

  public void addStepCategory(StepCategory stepCategory) {
    stepCategories.add(stepCategory);
  }

  public StepCategory getOrCreateChildStepCategory(String name) {
    Optional<StepCategory> stepCategoryOptional =
        stepCategories.stream().filter(category -> category.getName().equalsIgnoreCase(name)).findFirst();
    if (stepCategoryOptional.isPresent()) {
      return stepCategoryOptional.get();
    }
    StepCategory stepCategory = StepCategory.builder().name(name).build();
    addStepCategory(stepCategory);
    return stepCategory;
  }
}
