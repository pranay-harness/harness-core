package io.harness.cdng.pipeline.plancreators;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.cdng.executionplan.CDPlanCreatorType.INFRA_PLAN_CREATOR;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.executionplan.utils.PlanCreatorConfigUtils;
import io.harness.cdng.infra.steps.InfraStepParameters;
import io.harness.cdng.infra.steps.InfrastructureStep;
import io.harness.cdng.pipeline.CDStage;
import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.pipeline.steps.NGSectionStep;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.cdng.stepsdependency.utils.CDStepDependencyUtils;
import io.harness.exception.InvalidArgumentsException;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.ExecutionPlanCreatorResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.plancreator.beans.PlanCreatorConstants;
import io.harness.executionplan.stepsdependency.StepDependencyService;
import io.harness.executionplan.stepsdependency.instructors.OutcomeRefStepDependencyInstructor;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.pms.sdk.core.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.PlanNode.PlanNodeBuilder;
import io.harness.steps.section.chain.SectionChainStepParameters;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;

@OwnedBy(CDC)
public class InfraPlanCreator implements SupportDefinedExecutorPlanCreator<PipelineInfrastructure> {
  @Inject private StepDependencyService stepDependencyService;

  @Override
  public ExecutionPlanCreatorResponse createPlan(
      PipelineInfrastructure pipelineInfrastructure, ExecutionPlanCreationContext context) {
    PipelineInfrastructure actualInfraConfig = getActualInfraConfig(pipelineInfrastructure, context);
    PlanNode infraStepNode = getInfraStepNode(actualInfraConfig, context);
    PlanNode infraSectionNode = getInfraSectionNode(infraStepNode);
    return ExecutionPlanCreatorResponse.builder()
        .planNode(infraStepNode)
        .planNode(infraSectionNode)
        .startingNodeId(infraSectionNode.getUuid())
        .build();
  }

  private PlanNode getInfraStepNode(
      PipelineInfrastructure pipelineInfrastructure, ExecutionPlanCreationContext context) {
    final String infraNodeId = generateUuid();
    final String infraIdentifier = "infrastructureSpecification";

    PlanNodeBuilder planNodeBuilder =
        PlanNode.builder()
            .uuid(infraNodeId)
            .name(PlanCreatorConstants.INFRA_NODE_NAME)
            .identifier(infraIdentifier)
            .stepType(InfrastructureStep.STEP_TYPE)
            .skipExpressionChain(true)
            .stepParameters(InfraStepParameters.builder().pipelineInfrastructure(pipelineInfrastructure).build())
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                    .build());

    // Add step dependency provider.
    OutcomeRefStepDependencyInstructor instructor =
        OutcomeRefStepDependencyInstructor.builder()
            .key(CDStepDependencyUtils.getInfraKey(context))
            .providerPlanNodeId(infraNodeId)
            .outcomeExpression(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME)
            .build();
    stepDependencyService.registerStepDependencyInstructor(instructor, context);
    return planNodeBuilder.build();
  }

  private PlanNode getInfraSectionNode(PlanNode infraStepNode) {
    final String infraSectionNodeId = generateUuid();

    return PlanNode.builder()
        .uuid(infraSectionNodeId)
        .name(PlanCreatorConstants.INFRA_SECTION_NODE_IDENTIFIER)
        .identifier(PlanCreatorConstants.INFRA_SECTION_NODE_IDENTIFIER)
        .stepType(NGSectionStep.STEP_TYPE)
        .stepParameters(SectionChainStepParameters.builder().childNodeId(infraStepNode.getUuid()).build())
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD_CHAIN).build())
                .build())
        .skipGraphType(SkipType.SKIP_NODE)
        .build();
  }

  /** Method returns actual Infra object by resolving useFromStage if present. */
  private PipelineInfrastructure getActualInfraConfig(
      PipelineInfrastructure infrastructure, ExecutionPlanCreationContext context) {
    if (infrastructure.getUseFromStage() != null) {
      if (infrastructure.getInfrastructureDefinition() != null) {
        throw new InvalidArgumentsException("Infrastructure should not exist with UseFromStage.");
      }
      //  Add validation for not chaining of stages
      CDStage previousStage = PlanCreatorConfigUtils.getGivenDeploymentStageFromPipeline(
          context, infrastructure.getUseFromStage().getStage());
      if (previousStage != null) {
        DeploymentStage deploymentStage = (DeploymentStage) previousStage;
        return infrastructure.applyUseFromStage(deploymentStage.getInfrastructure());
      } else {
        throw new InvalidArgumentsException("Stage identifier given in useFromStage doesn't exist.");
      }
    }
    return infrastructure;
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
