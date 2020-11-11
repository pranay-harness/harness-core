package io.harness.pms.sdk.creator;

import io.harness.pms.plan.common.creator.PlanCreationContext;
import io.harness.pms.plan.common.creator.PlanCreationResponse;
import io.harness.pms.plan.common.yaml.YamlField;

public interface PartialPlanCreator {
  boolean supportsField(YamlField field);
  PlanCreationResponse createPlanForField(PlanCreationContext ctx, YamlField field);
}
