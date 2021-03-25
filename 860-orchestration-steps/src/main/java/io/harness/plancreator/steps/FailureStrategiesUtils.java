package io.harness.plancreator.steps;

import static io.harness.data.structure.HasPredicate.hasSome;

import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;
import io.harness.yaml.core.failurestrategy.NGFailureType;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
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

    if (hasSome(failureStrategyConfigList)) {
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
}
