package io.harness.pms.sdk.creator;

import java.util.Map;
import java.util.Set;

public interface PartialPlanCreator<T> {
  Class<T> getFieldClass();
  Map<String, Set<String>> getSupportedTypes();
  PlanCreationResponse createPlanForField(PlanCreationContext ctx, T field);
}
