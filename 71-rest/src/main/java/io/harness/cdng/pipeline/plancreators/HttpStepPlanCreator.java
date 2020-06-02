package io.harness.cdng.pipeline.plancreators;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.constants.PlanCreatorType.STEP_PLAN_CREATOR;

import com.google.inject.Singleton;

import io.harness.cdng.pipeline.stepinfo.HttpStepInfo;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import io.harness.redesign.states.http.BasicHttpStep;
import io.harness.redesign.states.http.BasicHttpStepParameters;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Singleton
@Slf4j
public class HttpStepPlanCreator implements SupportDefinedExecutorPlanCreator<HttpStepInfo> {
  @Override
  public CreateExecutionPlanResponse createPlan(HttpStepInfo httpStepInfo, CreateExecutionPlanContext context) {
    final PlanNode httpExecutionNode = prepareHttpExecutionNode(httpStepInfo, context);

    return CreateExecutionPlanResponse.builder()
        .planNode(httpExecutionNode)
        .startingNodeId(httpExecutionNode.getUuid())
        .build();
  }

  private PlanNode prepareHttpExecutionNode(HttpStepInfo httpStepInfo, CreateExecutionPlanContext context) {
    final String deploymentStageUid = generateUuid();

    final HttpStepInfo.HttpSpec spec = httpStepInfo.getSpec();
    return PlanNode.builder()
        .uuid(deploymentStageUid)
        .name(httpStepInfo.getIdentifier())
        .identifier(httpStepInfo.getIdentifier())
        .stepType(BasicHttpStep.STEP_TYPE)
        .stepParameters(BasicHttpStepParameters.builder().url(spec.getUrl()).method(spec.getMethod()).build())
        .facilitatorObtainment(
            FacilitatorObtainment.builder().type(FacilitatorType.builder().type(FacilitatorType.TASK).build()).build())
        .build();
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType())
        && searchContext.getObjectToPlan() instanceof HttpStepInfo;
  }

  @Override
  public List<String> getSupportedTypes() {
    return Collections.singletonList(STEP_PLAN_CREATOR.getName());
  }
}
