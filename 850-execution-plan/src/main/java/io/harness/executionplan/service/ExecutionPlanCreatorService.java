package io.harness.executionplan.service;

import io.harness.plan.Plan;
import io.harness.yaml.core.intfc.Pipeline;

import java.util.Map;

public interface ExecutionPlanCreatorService {
  Plan createPlanForPipeline(Pipeline pipeline, String accountId, Map<String, Object> contextAttributes);
}
