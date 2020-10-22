package io.harness.pipeline.plan.scratch.common.creator;

import io.harness.pipeline.plan.scratch.common.yaml.YamlField;

public interface PartialPlanCreator {
  boolean supportsField(YamlField field);
  PlanCreationResponse createPlanForField(PlanCreationContext ctx, YamlField field);
}
