package io.harness.pms.sdk.creator;

import io.harness.pms.plan.common.creator.PlanCreationContext;
import io.harness.pms.plan.common.creator.PlanCreationResponse;
import io.harness.pms.plan.common.yaml.YamlField;

import java.util.Map;
import java.util.Set;

public interface PartialPlanCreator {
  Map<String, Set<String>> getSupportedTypes();
  PlanCreationResponse createPlanForField(PlanCreationContext ctx, YamlField field);
}
