package io.harness.pms.sdk.creator;

import io.harness.pms.plan.creator.PlanCreationContext;
import io.harness.pms.plan.creator.PlanCreationResponse;

import java.util.Map;
import java.util.Set;

public interface PartialPlanCreator<T> {
  Class<T> getFieldClass();
  Map<String, Set<String>> getSupportedTypes();
  PlanCreationResponse createPlanForField(PlanCreationContext ctx, T field);
}
