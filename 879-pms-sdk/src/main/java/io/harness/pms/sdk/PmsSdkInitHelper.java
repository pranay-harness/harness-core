package io.harness.pms.sdk;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.plan.Types;
import io.harness.pms.sdk.core.plan.creation.creators.PartialPlanCreator;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class PmsSdkInitHelper {
  public static Map<String, Types> calculateSupportedTypes(PipelineServiceInfoProvider pipelineServiceInfoProvider) {
    List<PartialPlanCreator<?>> planCreators = pipelineServiceInfoProvider.getPlanCreators();
    if (EmptyPredicate.isEmpty(planCreators)) {
      return Collections.emptyMap();
    }

    Map<String, Set<String>> supportedTypes = new HashMap<>();
    for (PartialPlanCreator<?> planCreator : planCreators) {
      Map<String, Set<String>> currTypes = planCreator.getSupportedTypes();
      if (EmptyPredicate.isEmpty(currTypes)) {
        continue;
      }

      currTypes.forEach((k, v) -> {
        if (EmptyPredicate.isEmpty(v)) {
          return;
        }

        if (supportedTypes.containsKey(k)) {
          supportedTypes.get(k).addAll(v);
        } else {
          supportedTypes.put(k, new HashSet<>(v));
        }
      });
    }

    Map<String, Types> finalMap = new HashMap<>();
    supportedTypes.forEach((k, v) -> finalMap.put(k, Types.newBuilder().addAllTypes(v).build()));
    return finalMap;
  }
}
