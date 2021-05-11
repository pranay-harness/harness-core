package io.harness.pms.pipeline.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.pipeline.CommonStepInfo;
import io.harness.pms.pipeline.StepCategory;
import io.harness.pms.pipeline.StepData;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PIPELINE)
public class PMSPipelineServiceStepHelper {
  @Inject private final CommonStepInfo commonStepInfo;
  @VisibleForTesting static String LIBRARY = "Library";

  public static StepCategory calculateStepsForCategory(String module, List<StepInfo> stepInfoList) {
    StepCategory stepCategory = StepCategory.builder().name(module).build();
    for (StepInfo stepType : stepInfoList) {
      addToTopLevel(stepCategory, stepType);
    }
    return stepCategory;
  }

  public StepCategory calculateStepsForModuleBasedOnCategory(
      String category, List<StepInfo> stepInfoList, String accountId) {
    List<StepInfo> filteredStepTypes = new ArrayList<>();
    if (!stepInfoList.isEmpty()) {
      filteredStepTypes =
          stepInfoList.stream()
              .filter(stepInfo
                  -> EmptyPredicate.isEmpty(category) || stepInfo.getStepMetaData().getCategoryList().contains(category)
                      || EmptyPredicate.isEmpty(stepInfo.getStepMetaData().getCategoryList()))
              .collect(Collectors.toList());
    }
    filteredStepTypes.addAll(commonStepInfo.getCommonSteps(accountId));
    StepCategory stepCategory = StepCategory.builder().name(LIBRARY).build();
    for (StepInfo stepType : filteredStepTypes) {
      addToTopLevel(stepCategory, stepType);
    }
    return stepCategory;
  }

  public static void addToTopLevel(StepCategory stepCategory, StepInfo stepInfo) {
    StepCategory currentStepCategory = stepCategory;
    if (stepInfo != null) {
      String folderPath = stepInfo.getStepMetaData().getFolderPath();
      String[] categoryArrayName = folderPath.split("/");
      for (String categoryName : categoryArrayName) {
        currentStepCategory = currentStepCategory.getOrCreateChildStepCategory(categoryName);
      }
      currentStepCategory.addStepData(StepData.builder().name(stepInfo.getName()).type(stepInfo.getType()).build());
    }
  }
}
