package io.harness.executionplan.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.constants.PlanCreatorType.PIPELINE_PLAN_CREATOR;

import com.google.inject.Inject;

import io.harness.exception.NoResultFoundException;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.ExecutionPlanCreatorRegistry;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.impl.CreateExecutionPlanContextImpl;
import io.harness.executionplan.service.ExecutionPlanCreatorService;
import io.harness.plan.Plan;
import io.harness.yaml.core.intfc.Pipeline;

public class ExecutionPlanCreatorServiceImpl implements ExecutionPlanCreatorService {
  private final ExecutionPlanCreatorRegistry executionPlanCreatorRegistry;

  @Inject
  public ExecutionPlanCreatorServiceImpl(ExecutionPlanCreatorRegistry executionPlanCreatorRegistry) {
    this.executionPlanCreatorRegistry = executionPlanCreatorRegistry;
  }

  @Override
  public Plan createPlanForPipeline(Pipeline pipeline, String accountId) {
    final CreateExecutionPlanContext createExecutionPlanContext =
        CreateExecutionPlanContextImpl.builder().accountId(accountId).build();

    final PlanCreatorSearchContext<Pipeline> searchContext = PlanCreatorSearchContext.<Pipeline>builder()
                                                                 .objectToPlan(pipeline)
                                                                 .type(PIPELINE_PLAN_CREATOR.getName())
                                                                 .createExecutionPlanContext(createExecutionPlanContext)
                                                                 .build();

    final ExecutionPlanCreator<Pipeline> planCreator =
        executionPlanCreatorRegistry.obtainCreator(searchContext)
            .orElseThrow(()
                             -> NoResultFoundException.newBuilder()
                                    .message("no execution plan creator found for pipeline")
                                    .build());

    final CreateExecutionPlanResponse response = planCreator.createPlan(pipeline, createExecutionPlanContext);

    return Plan.builder()
        .nodes(response.getPlanNodes())
        .startingNodeId(response.getStartingNodeId())
        .uuid(generateUuid())
        .build();
  }
}
