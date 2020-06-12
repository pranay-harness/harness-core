package io.harness.cdng.infra;

import static io.harness.cdng.executionplan.CDPlanCreatorType.INFRA_PLAN_CREATOR;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.cdng.environment.steps.EnvironmentStep;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.infra.steps.InfrastructureStep;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import io.harness.state.core.section.chain.SectionChainStep;
import io.harness.state.core.section.chain.SectionChainStepParameters;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

public class InfraPlanCreator implements SupportDefinedExecutorPlanCreator<PipelineInfrastructure> {
  @Override
  public CreateExecutionPlanResponse createPlan(
      PipelineInfrastructure pipelineInfrastructure, CreateExecutionPlanContext context) {
    PlanNode infraStepNode = getInfraStepNode(pipelineInfrastructure);
    PlanNode envStepNode = getEnvStepNode(pipelineInfrastructure);
    PlanNode infraSectionNode = getInfraSectionNode(pipelineInfrastructure, infraStepNode, envStepNode);
    return CreateExecutionPlanResponse.builder()
        .planNode(envStepNode)
        .planNode(infraStepNode)
        .planNode(infraSectionNode)
        .startingNodeId(infraSectionNode.getUuid())
        .build();
  }

  private PlanNode getEnvStepNode(PipelineInfrastructure pipelineInfrastructure) {
    final String envNodeId = generateUuid();
    EnvironmentYaml environment = pipelineInfrastructure.getEnvironment();
    environment.setDisplayName(StringUtils.defaultIfEmpty(environment.getDisplayName(), environment.getIdentifier()));

    return PlanNode.builder()
        .uuid(envNodeId)
        .name(environment.getDisplayName())
        .identifier(environment.getIdentifier())
        .stepType(EnvironmentStep.STEP_TYPE)
        .facilitatorObtainment(
            FacilitatorObtainment.builder().type(FacilitatorType.builder().type(FacilitatorType.SYNC).build()).build())
        .stepParameters(environment)
        .build();
  }

  private PlanNode getInfraStepNode(PipelineInfrastructure pipelineInfrastructure) {
    final String infraNodeId = generateUuid();
    final String infraIdentifier = "infrastructureSpecification";

    return PlanNode.builder()
        .uuid(infraNodeId)
        .name(infraIdentifier)
        .identifier(infraIdentifier)
        .stepType(InfrastructureStep.STEP_TYPE)
        .stepParameters(pipelineInfrastructure.getInfrastructureSpec().getInfrastructure())
        .facilitatorObtainment(
            FacilitatorObtainment.builder().type(FacilitatorType.builder().type(FacilitatorType.SYNC).build()).build())
        .build();
  }

  private PlanNode getInfraSectionNode(
      PipelineInfrastructure pipelineInfrastructure, PlanNode infraStepNode, PlanNode envNode) {
    final String infraSectionNodeId = generateUuid();
    final String infraSectionIdentifier = "infrastructure";

    return PlanNode.builder()
        .uuid(infraSectionNodeId)
        .name(infraSectionIdentifier)
        .identifier(infraSectionIdentifier)
        .stepType(SectionChainStep.STEP_TYPE)
        .stepParameters(SectionChainStepParameters.builder()
                            .childNodeId(envNode.getUuid())
                            .childNodeId(infraStepNode.getUuid())
                            .build())
        .facilitatorObtainment(FacilitatorObtainment.builder()
                                   .type(FacilitatorType.builder().type(FacilitatorType.CHILD_CHAIN).build())
                                   .build())
        .build();
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType())
        && searchContext.getObjectToPlan() instanceof PipelineInfrastructure;
  }

  @Override
  public List<String> getSupportedTypes() {
    return Collections.singletonList(INFRA_PLAN_CREATOR.getName());
  }
}
