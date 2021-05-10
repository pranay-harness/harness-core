package io.harness.plancreator.steps;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;
import io.harness.yaml.core.failurestrategy.NGFailureActionType;
import io.harness.yaml.core.failurestrategy.NGFailureType;
import io.harness.yaml.core.failurestrategy.manualintervention.ManualFailureSpecConfig;
import io.harness.yaml.core.failurestrategy.manualintervention.ManualInterventionFailureActionConfig;
import io.harness.yaml.core.failurestrategy.retry.RetryFailureActionConfig;
import io.harness.yaml.core.failurestrategy.retry.RetryFailureSpecConfig;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PIPELINE)
@TargetModule(HarnessModule._882_PMS_SDK_CORE)
public class FailureStrategiesUtils {
  public Map<FailureStrategyActionConfig, Collection<FailureType>> priorityMergeFailureStrategies(
      List<FailureStrategyConfig> stepFailureStrategies, List<FailureStrategyConfig> stepGroupFailureStrategies,
      List<FailureStrategyConfig> stageFailureStrategies) {
    // priority merge all declared failure strategies, least significant are added first to map
    EnumMap<NGFailureType, FailureStrategyActionConfig> failureStrategiesMap = new EnumMap<>(NGFailureType.class);
    failureStrategiesMap.putAll(expandFailureStrategiesToMap(stageFailureStrategies));
    failureStrategiesMap.putAll(expandFailureStrategiesToMap(stepGroupFailureStrategies));
    failureStrategiesMap.putAll(expandFailureStrategiesToMap(stepFailureStrategies));

    // invert map so that action become key
    return convertNGFailureTypeToFailureTypesMultiMap(failureStrategiesMap);
  }

  private EnumMap<NGFailureType, FailureStrategyActionConfig> expandFailureStrategiesToMap(
      List<FailureStrategyConfig> failureStrategyConfigList) {
    EnumMap<NGFailureType, FailureStrategyActionConfig> map = new EnumMap<>(NGFailureType.class);

    if (isNotEmpty(failureStrategyConfigList)) {
      for (FailureStrategyConfig failureStrategyConfig : failureStrategyConfigList) {
        for (NGFailureType ngFailureType : failureStrategyConfig.getOnFailure().getErrors()) {
          if (map.containsKey(ngFailureType)
              && !map.get(ngFailureType).equals(failureStrategyConfig.getOnFailure().getAction())) {
            throw new InvalidRequestException(
                "Same error cannot point to multiple failure action - for error : " + ngFailureType.getYamlName());
          }
          map.put(ngFailureType, failureStrategyConfig.getOnFailure().getAction());
        }
      }
    }
    return map;
  }

  private Map<FailureStrategyActionConfig, Collection<FailureType>> convertNGFailureTypeToFailureTypesMultiMap(
      EnumMap<NGFailureType, FailureStrategyActionConfig> map) {
    Multimap<FailureStrategyActionConfig, FailureType> invertedMap = ArrayListMultimap.create();

    // Handle Other_Errors
    if (map.containsKey(NGFailureType.ANY_OTHER_ERRORS)) {
      EnumSet<FailureType> requiredFailureTypes = NGFailureType.getAllFailureTypes();
      EnumSet<FailureType> mentionedFailureTypes = EnumSet.noneOf(FailureType.class);
      map.keySet().forEach(ngFailureType -> {
        EnumSet<FailureType> failureTypes = ngFailureType.getFailureTypes();
        mentionedFailureTypes.addAll(failureTypes);
        failureTypes.forEach(failureType -> invertedMap.put(map.get(ngFailureType), failureType));
      });
      mentionedFailureTypes.forEach(requiredFailureTypes::remove);
      invertedMap.putAll(map.get(NGFailureType.ANY_OTHER_ERRORS), requiredFailureTypes);
    } else {
      map.keySet().forEach(ngFailureType -> {
        EnumSet<FailureType> failureTypes = ngFailureType.getFailureTypes();
        failureTypes.forEach(failureType -> invertedMap.put(map.get(ngFailureType), failureType));
      });
    }
    return invertedMap.asMap();
  }

  public void validateRetryFailureAction(RetryFailureActionConfig retryAction) {
    ParameterField<Integer> retryCount = retryAction.getSpecConfig().getRetryCount();
    if (retryCount.isExpression()) {
      throw new InvalidRequestException("RetryCount fixed value is not given.");
    }
    if (retryAction.getSpecConfig().getRetryIntervals().isExpression()) {
      throw new InvalidRequestException("RetryIntervals cannot be expression/runtime input. Please give values.");
    }

    FailureStrategyActionConfig actionUnderRetry = retryAction.getSpecConfig().getOnRetryFailure().getAction();

    if (!validateActionAfterRetryFailure(actionUnderRetry)) {
      throw new InvalidRequestException("Retry action cannot have post retry failure action as Retry");
    }
    // validating Retry -> Manual Intervention -> Retry
    if (actionUnderRetry.getType().equals(NGFailureActionType.MANUAL_INTERVENTION)) {
      if (validateRetryActionUnderManualAction(
              ((ManualInterventionFailureActionConfig) actionUnderRetry).getSpecConfig())) {
        throw new InvalidRequestException(
            "Retry Action cannot be applied under Manual Action which itself is in Retry Action");
      }
    }
  }

  public void validateManualInterventionFailureAction(ManualInterventionFailureActionConfig actionConfig) {
    FailureStrategyActionConfig actionUnderManualIntervention = actionConfig.getSpecConfig().getOnTimeout().getAction();
    if (actionUnderManualIntervention.getType().equals(NGFailureActionType.MANUAL_INTERVENTION)) {
      throw new InvalidRequestException("Manual Action cannot be applied as PostTimeOut Action");
    }
    // validating Manual Intervention -> Retry -> Manual Intervention
    if (actionUnderManualIntervention.getType().equals(NGFailureActionType.RETRY)) {
      if (FailureStrategiesUtils.validateManualActionUnderRetryAction(
              ((RetryFailureActionConfig) actionUnderManualIntervention).getSpecConfig())) {
        throw new InvalidRequestException(
            "Manual Action cannot be applied under Retry Action which itself is in Manual Action");
      }
    }
  }

  public boolean validateActionAfterRetryFailure(FailureStrategyActionConfig action) {
    return action.getType() != NGFailureActionType.RETRY;
  }

  public boolean validateManualActionUnderRetryAction(RetryFailureSpecConfig retrySpecConfig) {
    return retrySpecConfig.getOnRetryFailure().getAction().getType().equals(NGFailureActionType.MANUAL_INTERVENTION);
  }

  public boolean validateRetryActionUnderManualAction(ManualFailureSpecConfig manualSpecConfig) {
    return manualSpecConfig.getOnTimeout().getAction().getType().equals(NGFailureActionType.RETRY);
  }
}
