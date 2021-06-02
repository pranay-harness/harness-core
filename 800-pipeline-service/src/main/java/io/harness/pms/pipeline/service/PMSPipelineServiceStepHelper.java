package io.harness.pms.pipeline.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.helpers.PmsFeatureFlagHelper;
import io.harness.pms.pipeline.CommonStepInfo;
import io.harness.pms.pipeline.StepCategory;
import io.harness.pms.pipeline.StepData;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PIPELINE)
public class PMSPipelineServiceStepHelper {
  @Inject private final PmsFeatureFlagHelper pmsFeatureFlagHelper;
  @Inject private final CommonStepInfo commonStepInfo;
  @VisibleForTesting static String LIBRARY = "Library";

  private static final int CACHE_EVICTION_TIME_HOUR = 2;

  private final LoadingCache<String, List<String>> featureFlagCache =
      CacheBuilder.newBuilder()
          .expireAfterAccess(CACHE_EVICTION_TIME_HOUR, TimeUnit.HOURS)
          .build(new CacheLoader<String, List<String>>() {
            @Override
            public List<String> load(@NotNull final String accountId) {
              return pmsFeatureFlagHelper.listAllEnabledFeatureFlagsForAccount(accountId);
            }
          });

  public List<StepInfo> filterStepsOnFeatureFlag(List<StepInfo> stepInfoList, String accountId) {
    try {
      List<String> featureFlagsForAccount = featureFlagCache.get(accountId);
      List<StepInfo> ffEnabledStepInfoList = new ArrayList<>();
      if (!stepInfoList.isEmpty()) {
        ffEnabledStepInfoList = stepInfoList.stream()
                                    .filter(stepInfo
                                        -> EmptyPredicate.isEmpty(stepInfo.getFeatureFlag())
                                            || featureFlagsForAccount.contains(stepInfo.getFeatureFlag()))
                                    .collect(Collectors.toList());
      }
      return ffEnabledStepInfoList;
    } catch (Exception ex) {
      log.error(ex.getMessage());
      throw new InvalidRequestException(String.format("Could not fetch feature flags for accountID: %s", accountId));
    }
  }

  public StepCategory calculateStepsForCategory(String module, List<StepInfo> stepInfoList, String accountId) {
    List<StepInfo> ffEnabledStepInfoList = filterStepsOnFeatureFlag(stepInfoList, accountId);
    StepCategory stepCategory = StepCategory.builder().name(module).build();
    for (StepInfo stepType : ffEnabledStepInfoList) {
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
    filteredStepTypes.addAll(commonStepInfo.getCommonSteps());
    return calculateStepsForCategory(LIBRARY, filteredStepTypes, accountId);
  }

  public void addToTopLevel(StepCategory stepCategory, StepInfo stepInfo) {
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
