package io.harness.pms.pipeline.service;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.enforcement.client.services.EnforcementClientService;
import io.harness.enforcement.constants.FeatureRestrictionName;
import io.harness.pms.contracts.steps.SdkStep;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.pipeline.CommonStepInfo;
import io.harness.pms.sdk.PmsSdkInstanceService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class PipelineEnforcementServiceImpl implements PipelineEnforcementService {
  @Inject PmsSdkInstanceService pmsSdkInstanceService;
  @Inject EnforcementClientService enforcementClientService;
  @Inject CommonStepInfo commonStepInfo;

  @Override
  public boolean isFeatureRestricted(String accountId, String featureRestrictionName) {
    return enforcementClientService.isAvailable(FeatureRestrictionName.valueOf(featureRestrictionName), accountId);
  }

  @Override
  public Map<FeatureRestrictionName, Boolean> getFeatureRestrictionMap(
      String accountId, List<String> featureRestrictionNameList) {
    List<FeatureRestrictionName> featureRestrictionNames =
        featureRestrictionNameList.stream().map(FeatureRestrictionName::valueOf).collect(Collectors.toList());
    Map<FeatureRestrictionName, Boolean> featureRestrictionNameBooleanMap = new HashMap<>();
    // Todo: Sync up with GTM on having a list api
    for (FeatureRestrictionName featureRestrictionName : featureRestrictionNames) {
      featureRestrictionNameBooleanMap.put(
          featureRestrictionName, enforcementClientService.isAvailable(featureRestrictionName, accountId));
    }
    return featureRestrictionNameBooleanMap;
  }

  @Override
  public void checkFeatureRestrictionOrThrow(String accountId, List<String> featureRestrictionNameList) {
    // Todo: Sync up with GTM on having a list api
    for (String featureRestrictionName : featureRestrictionNameList) {
      enforcementClientService.checkAvailability(FeatureRestrictionName.valueOf(featureRestrictionName), accountId);
    }
  }

  /**
   * NOTE: Use this function during execution only.
   * @param accountId
   * @param stepTypes
   */
  @Override
  public void checkStepRestrictionAndThrow(String accountId, List<StepType> stepTypes) {
    Map<String, Set<SdkStep>> sdkSteps = pmsSdkInstanceService.getSdkSteps();
    Set<String> featureRestrictionNameList = new HashSet<>();
    Set<String> modules = new HashSet<>();
    for (Map.Entry<String, Set<SdkStep>> entry : sdkSteps.entrySet()) {
      for (SdkStep sdkStep : entry.getValue()) {
        if (stepTypes.contains(sdkStep.getStepType())) {
          if (sdkStep.hasStepInfo() && EmptyPredicate.isNotEmpty(sdkStep.getStepInfo().getFeatureRestrictionName())) {
            featureRestrictionNameList.add(sdkStep.getStepInfo().getFeatureRestrictionName());
          }
          if (sdkStep.getStepType().getStepCategory() == StepCategory.STAGE) {
            modules.add(entry.getKey());
          }
        }
      }
    }
    List<String> stepTypeString = stepTypes.stream().map(StepType::getType).collect(Collectors.toList());
    for (StepInfo stepInfo : commonStepInfo.getCommonSteps("")) {
      if (stepTypeString.contains(stepInfo.getType())
          && EmptyPredicate.isNotEmpty(stepInfo.getFeatureRestrictionName())) {
        featureRestrictionNameList.add(stepInfo.getFeatureRestrictionName());
      }
    }
    for (String module : modules) {
      if (module.equalsIgnoreCase(ModuleType.CD.name())) {
        featureRestrictionNameList.add(FeatureRestrictionName.DEPLOYMENTS.name());
      } else if (module.equalsIgnoreCase(ModuleType.CI.name())) {
        featureRestrictionNameList.add(FeatureRestrictionName.BUILDS.name());
      }
    }

    checkFeatureRestrictionOrThrow(accountId, new ArrayList<>(featureRestrictionNameList));
  }
}
