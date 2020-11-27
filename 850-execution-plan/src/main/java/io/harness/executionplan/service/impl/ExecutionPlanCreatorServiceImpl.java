package io.harness.executionplan.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.PIPELINE_PLAN_CREATOR;

import io.harness.exception.NoResultFoundException;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.ExecutionPlanCreatorRegistry;
import io.harness.executionplan.core.ExecutionPlanCreatorResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.impl.ExecutionPlanCreationContextImpl;
import io.harness.executionplan.service.ExecutionPlanCreatorService;
import io.harness.plan.Plan;
import io.harness.yaml.core.intfc.Pipeline;

import com.google.inject.Inject;
import java.util.Map;

public class ExecutionPlanCreatorServiceImpl implements ExecutionPlanCreatorService {
  private final ExecutionPlanCreatorRegistry executionPlanCreatorRegistry;

  @Inject
  public ExecutionPlanCreatorServiceImpl(ExecutionPlanCreatorRegistry executionPlanCreatorRegistry) {
    this.executionPlanCreatorRegistry = executionPlanCreatorRegistry;
  }

  @Override
  public Plan createPlanForPipeline(Pipeline pipeline, String accountId, Map<String, Object> contextAttributes) {
    final ExecutionPlanCreationContext executionPlanCreationContext =
        ExecutionPlanCreationContextImpl.builder().accountId(accountId).build();

    if (isNotEmpty(contextAttributes)) {
      for (Map.Entry<String, Object> entry : contextAttributes.entrySet()) {
        executionPlanCreationContext.addAttribute(entry.getKey(), entry.getValue());
      }
    }

    final PlanCreatorSearchContext<Pipeline> searchContext =
        PlanCreatorSearchContext.<Pipeline>builder()
            .objectToPlan(pipeline)
            .type(PIPELINE_PLAN_CREATOR.getName())
            .createExecutionPlanContext(executionPlanCreationContext)
            .build();

    final ExecutionPlanCreator<Pipeline> planCreator =
        executionPlanCreatorRegistry.obtainCreator(searchContext)
            .orElseThrow(()
                             -> NoResultFoundException.newBuilder()
                                    .message("no execution plan creator found for pipeline")
                                    .build());

    final ExecutionPlanCreatorResponse response = planCreator.createPlan(pipeline, executionPlanCreationContext);

    return Plan.builder().nodes(response.getPlanNodes()).startingNodeId(response.getStartingNodeId()).build();
  }
}
