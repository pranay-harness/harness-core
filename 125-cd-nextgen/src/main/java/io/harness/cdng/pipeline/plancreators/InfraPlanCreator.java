package io.harness.cdng.pipeline.plancreators;

import static io.harness.cdng.executionplan.CDPlanCreatorType.INFRA_PLAN_CREATOR;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;

import io.harness.cdng.environment.steps.EnvironmentStep;
import io.harness.cdng.environment.steps.EnvironmentStepParameters;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.executionplan.utils.PlanCreatorConfigUtils;
import io.harness.cdng.infra.steps.InfraStepParameters;
import io.harness.cdng.infra.steps.InfrastructureStep;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.pipeline.CDStage;
import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.cdng.stepsdependency.utils.CDStepDependencyUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidArgumentsException;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.ExecutionPlanCreatorResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.plancreator.beans.PlanCreatorConstants;
import io.harness.executionplan.stepsdependency.StepDependencyService;
import io.harness.executionplan.stepsdependency.instructors.OutcomeRefStepDependencyInstructor;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import io.harness.plan.PlanNode.PlanNodeBuilder;
import io.harness.steps.section.chain.SectionChainStep;
import io.harness.steps.section.chain.SectionChainStepParameters;

import java.util.Collections;
import java.util.List;

public class InfraPlanCreator implements SupportDefinedExecutorPlanCreator<PipelineInfrastructure> {
  @Inject private StepDependencyService stepDependencyService;

  @Override
  public ExecutionPlanCreatorResponse createPlan(
      PipelineInfrastructure pipelineInfrastructure, ExecutionPlanCreationContext context) {
    PipelineInfrastructure actualInfraConfig = getActualInfraConfig(pipelineInfrastructure, context);
    PlanNode infraStepNode = getInfraStepNode(actualInfraConfig, context);
    PlanNode envStepNode = getEnvStepNode(actualInfraConfig);
    PlanNode infraSectionNode = getInfraSectionNode(infraStepNode, envStepNode);
    return ExecutionPlanCreatorResponse.builder()
        .planNode(envStepNode)
        .planNode(infraStepNode)
        .planNode(infraSectionNode)
        .startingNodeId(infraSectionNode.getUuid())
        .build();
  }

  private PlanNode getEnvStepNode(PipelineInfrastructure pipelineInfrastructure) {
    final String envNodeId = generateUuid();
    EnvironmentYaml environment = pipelineInfrastructure.getEnvironment();
    if (!environment.getName().isExpression() && EmptyPredicate.isEmpty(environment.getName().getValue())) {
      environment.setName(environment.getIdentifier());
    }
    EnvironmentYaml environmentOverrides = null;
    if (pipelineInfrastructure.getUseFromStage() != null
        && pipelineInfrastructure.getUseFromStage().getOverrides() != null) {
      environmentOverrides = pipelineInfrastructure.getUseFromStage().getOverrides().getEnvironment();
      if (!environmentOverrides.getName().isExpression()
          && EmptyPredicate.isEmpty(environmentOverrides.getName().getValue())) {
        environmentOverrides.setName(environmentOverrides.getIdentifier());
      }
    }

    final String environmentIdentifier = "environment";
    return PlanNode.builder()
        .uuid(envNodeId)
        .name(environmentIdentifier)
        .identifier(environmentIdentifier)
        .stepType(EnvironmentStep.STEP_TYPE)
        .facilitatorObtainment(
            FacilitatorObtainment.builder().type(FacilitatorType.builder().type(FacilitatorType.SYNC).build()).build())
        .stepParameters(EnvironmentStepParameters.builder()
                            .environment(environment)
                            .environmentOverrides(environmentOverrides)
                            .build())
        .build();
  }

  private PlanNode getInfraStepNode(
      PipelineInfrastructure pipelineInfrastructure, ExecutionPlanCreationContext context) {
    final String infraNodeId = generateUuid();
    final String infraIdentifier = "infrastructureSpecification";

    Infrastructure infraOverrides = null;
    if (pipelineInfrastructure.getUseFromStage() != null
        && pipelineInfrastructure.getUseFromStage().getOverrides() != null
        && pipelineInfrastructure.getUseFromStage().getOverrides().getInfrastructureDefinition() != null) {
      infraOverrides =
          pipelineInfrastructure.getUseFromStage().getOverrides().getInfrastructureDefinition().getInfrastructure();
    }

    PlanNodeBuilder planNodeBuilder =
        PlanNode.builder()
            .uuid(infraNodeId)
            .name(infraIdentifier)
            .identifier(infraIdentifier)
            .stepType(InfrastructureStep.STEP_TYPE)
            .stepParameters(
                InfraStepParameters.builder()
                    .infrastructure(pipelineInfrastructure.getInfrastructureDefinition().getInfrastructure())
                    .infrastructureOverrides(infraOverrides)
                    .build())
            .facilitatorObtainment(FacilitatorObtainment.builder()
                                       .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                       .build());

    // Add step dependency provider.
    OutcomeRefStepDependencyInstructor instructor = OutcomeRefStepDependencyInstructor.builder()
                                                        .key(CDStepDependencyUtils.getInfraKey(context))
                                                        .providerPlanNodeId(infraNodeId)
                                                        .outcomeExpression(OutcomeExpressionConstants.INFRASTRUCTURE)
                                                        .build();
    stepDependencyService.registerStepDependencyInstructor(instructor, context);
    return planNodeBuilder.build();
  }

  private PlanNode getInfraSectionNode(PlanNode infraStepNode, PlanNode envNode) {
    final String infraSectionNodeId = generateUuid();

    return PlanNode.builder()
        .uuid(infraSectionNodeId)
        .name(PlanCreatorConstants.INFRA_SECTION_NODE_IDENTIFIER)
        .identifier(PlanCreatorConstants.INFRA_SECTION_NODE_IDENTIFIER)
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
