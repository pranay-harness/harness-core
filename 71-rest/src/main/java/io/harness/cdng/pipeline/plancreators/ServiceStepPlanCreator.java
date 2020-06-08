package io.harness.cdng.pipeline.plancreators;

import static io.harness.cdng.executionplan.CDPlanCreatorType.ARTIFACT_PLAN_CREATOR;
import static io.harness.cdng.executionplan.CDPlanCreatorType.SERVICE_PLAN_CREATOR;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.service.Service;
import io.harness.cdng.service.steps.ServiceStep;
import io.harness.cdng.service.steps.ServiceStepParameters;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
@Slf4j
public class ServiceStepPlanCreator implements SupportDefinedExecutorPlanCreator<Service> {
  @Inject private ExecutionPlanCreatorHelper executionPlanCreatorHelper;

  @Override
  public CreateExecutionPlanResponse createPlan(Service service, CreateExecutionPlanContext context) {
    final CreateExecutionPlanResponse planForArtifacts =
        getPlanForArtifacts(context, service.getServiceSpec().getArtifacts());
    List<String> childNodeIds = new ArrayList<>();
    List<PlanNode> planNodes = new ArrayList<>(planForArtifacts.getPlanNodes());
    childNodeIds.add(planForArtifacts.getStartingNodeId());
    final PlanNode serviceExecutionNode = prepareServiceNode(service, childNodeIds);
    return CreateExecutionPlanResponse.builder()
        .planNode(serviceExecutionNode)
        .planNodes(planNodes)
        .startingNodeId(serviceExecutionNode.getUuid())
        .build();
  }

  private PlanNode prepareServiceNode(Service service, List<String> childNodeIds) {
    final String serviceNodeUid = generateUuid();

    return PlanNode.builder()
        .uuid(serviceNodeUid)
        .name(service.getDisplayName())
        .identifier(service.getIdentifier())
        .stepType(ServiceStep.STEP_TYPE)
        .stepParameters(ServiceStepParameters.builder().parallelNodeIds(childNodeIds).service(service).build())
        .facilitatorObtainment(FacilitatorObtainment.builder()
                                   .type(FacilitatorType.builder().type(FacilitatorType.CHILDREN).build())
                                   .build())
        .build();
  }

  private CreateExecutionPlanResponse getPlanForArtifacts(
      CreateExecutionPlanContext context, ArtifactListConfig artifactListConfig) {
    final ExecutionPlanCreator<ArtifactListConfig> executionPlanCreator =
        executionPlanCreatorHelper.getExecutionPlanCreator(ARTIFACT_PLAN_CREATOR.getName(), artifactListConfig, context,
            "No execution plan creator found for artifacts execution");

    return executionPlanCreator.createPlan(artifactListConfig, context);
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType()) && searchContext.getObjectToPlan() instanceof Service;
  }

  @Override
  public List<String> getSupportedTypes() {
    return Collections.singletonList(SERVICE_PLAN_CREATOR.getName());
  }
}
