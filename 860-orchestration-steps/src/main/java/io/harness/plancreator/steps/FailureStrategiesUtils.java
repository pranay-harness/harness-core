package io.harness.plancreator.steps;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.yaml.core.failurestrategy.FailureStrategyActionConfig;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.assertj.core.util.VisibleForTesting;

@UtilityClass
public class FailureStrategiesUtils {
  public Map<FailureStrategyActionConfig, Collection<FailureType>> priorityMergeFailureStrategies(
      List<FailureStrategyConfig> stepFailureStrategies, List<FailureStrategyConfig> stepGroupFailureStrategies,
      List<FailureStrategyConfig> stageFailureStrategies) {
    // priority merge all declared failure strategies, least significant are added first to map
    EnumMap<FailureType, FailureStrategyActionConfig> map = new EnumMap<>(FailureType.class);
    map.putAll(expandFailureStrategiesToMap(stageFailureStrategies));
    map.putAll(expandFailureStrategiesToMap(stepGroupFailureStrategies));
    map.putAll(expandFailureStrategiesToMap(stepFailureStrategies));

    // invert map so that action become key
    Multimap<FailureStrategyActionConfig, FailureType> inverted =
        Multimaps.invertFrom(Multimaps.forMap(map), ArrayListMultimap.create());
    return inverted.asMap();
  }

  @VisibleForTesting
  private EnumMap<FailureType, FailureStrategyActionConfig> expandFailureStrategiesToMap(
      List<FailureStrategyConfig> failureStrategyConfigList) {
    EnumMap<FailureType, FailureStrategyActionConfig> map = new EnumMap<>(FailureType.class);

    if (isNotEmpty(failureStrategyConfigList)) {
      for (FailureStrategyConfig failureStrategyConfig : failureStrategyConfigList) {
        failureStrategyConfig.getOnFailure()
            .getErrors()
            .stream()
            .flatMap(ngFailureType -> ngFailureType.getFailureType().stream())
            .forEach(ngFailureType -> map.put(ngFailureType, failureStrategyConfig.getOnFailure().getAction()));
      }
    }
    return map;
  }
}
