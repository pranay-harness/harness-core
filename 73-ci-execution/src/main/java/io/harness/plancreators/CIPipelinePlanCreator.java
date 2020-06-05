package io.harness.plancreators;

import static io.harness.executionplan.plancreator.beans.PlanCreatorType.PIPELINE_PLAN_CREATOR;
import static io.harness.executionplan.plancreator.beans.PlanCreatorType.STAGES_PLAN_CREATOR;
import static java.util.Collections.singletonList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.CIPipeline;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.yaml.core.auxiliary.intfc.StageWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * CI pipeline plan creator
 * CI Pipeline does not require separate step, it is creating plan for stages directly and returning it
 */
@Singleton
@Slf4j
public class CIPipelinePlanCreator implements SupportDefinedExecutorPlanCreator<CIPipeline> {
  @Inject private ExecutionPlanCreatorHelper executionPlanCreatorHelper;
  @Override
  public CreateExecutionPlanResponse createPlan(CIPipeline ciPipeline, CreateExecutionPlanContext context) {
    addArgumentsToContext(ciPipeline, context);
    return createPlanForStages(ciPipeline.getStages(), context);
  }

  private void addArgumentsToContext(CIPipeline pipeline, CreateExecutionPlanContext context) {
    context.addAttribute("CI_PIPELINE_CONFIG", pipeline);
  }

  private CreateExecutionPlanResponse createPlanForStages(
      List<? extends StageWrapper> stages, CreateExecutionPlanContext context) {
    final ExecutionPlanCreator<List<? extends StageWrapper>> stagesPlanCreator =
        executionPlanCreatorHelper.getExecutionPlanCreator(
            STAGES_PLAN_CREATOR.getName(), stages, context, "no execution plan creator found for pipeline stages");

    return stagesPlanCreator.createPlan(stages, context);
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType())
        && searchContext.getObjectToPlan() instanceof CIPipeline;
  }

  @Override
  public List<String> getSupportedTypes() {
    return singletonList(PIPELINE_PLAN_CREATOR.getName());
  }
}
