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
import io.harness.exception.InvalidArgumentsException;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.stepsdependency.StepDependencyService;
import io.harness.executionplan.stepsdependency.instructors.OutcomeRefStepDependencyInstructor;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import io.harness.plan.PlanNode.PlanNodeBuilder;
import io.harness.state.core.section.chain.SectionChainStep;
import io.harness.state.core.section.chain.SectionChainStepParameters;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

public class InfraPlanCreator implements SupportDefinedExecutorPlanCreator<PipelineInfrastructure> {
  @Inject private StepDependencyService stepDependencyService;

  @Override
  public CreateExecutionPlanResponse createPlan(
      PipelineInfrastructure pipelineInfrastructure, CreateExecutionPlanContext context) {
    PipelineInfrastructure actualInfraConfig = getActualInfraConfig(pipelineInfrastructure, context);
    PlanNode infraStepNode = getInfraStepNode(actualInfraConfig, context);
    PlanNode envStepNode = getEnvStepNode(actualInfraConfig);
    PlanNode infraSectionNode = getInfraSectionNode(infraStepNode, envStepNode);
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
    EnvironmentYaml environmentOverrides = null;
    if (pipelineInfrastructure.getUseFromStage() != null
        && pipelineInfrastructure.getUseFromStage().getOverrides() != null) {
      environmentOverrides = pipelineInfrastructure.getUseFromStage().getOverrides().getEnvironment();
      environmentOverrides.setDisplayName(
          StringUtils.defaultIfEmpty(environment.getDisplayName(), environment.getIdentifier()));
    }

    return PlanNode.builder()
        .uuid(envNodeId)
        .name(environment.getDisplayName())
        .identifier(environment.getIdentifier())
        .stepType(EnvironmentStep.STEP_TYPE)
        .facilitatorObtainment(
            FacilitatorObtainment.builder().type(FacilitatorType.builder().type(FacilitatorType.SYNC).build()).build())
        .stepParameters(EnvironmentStepParameters.builder()
                            .environment(environment)
                            .environmentOverrides(environmentOverrides)
                            .build())
        .build();
  }

  private PlanNode getInfraStepNode(PipelineInfrastructure pipelineInfrastructure, CreateExecutionPlanContext context) {
    final String infraNodeId = generateUuid();
    final String infraIdentifier = "infrastructureSpecification";

    Infrastructure infraOverrides = null;
    if (pipelineInfrastructure.getUseFromStage() != null
        && pipelineInfrastructure.getUseFromStage().getOverrides() != null
        && pipelineInfrastructure.getUseFromStage().getOverrides().getInfrastructureSpec() != null) {
      infraOverrides =
          pipelineInfrastructure.getUseFromStage().getOverrides().getInfrastructureSpec().getInfrastructure();
    }

    PlanNodeBuilder planNodeBuilder =
        PlanNode.builder()
            .uuid(infraNodeId)
            .name(infraIdentifier)
            .identifier(infraIdentifier)
            .stepType(InfrastructureStep.STEP_TYPE)
            .stepParameters(InfraStepParameters.builder()
                                .infrastructure(pipelineInfrastructure.getInfrastructureSpec().getInfrastructure())
                                .infrastructureOverrides(infraOverrides)
                                .build())
            .facilitatorObtainment(FacilitatorObtainment.builder()
                                       .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                       .build());

    // Add step dependency provider.
    OutcomeRefStepDependencyInstructor instructor =
        OutcomeRefStepDependencyInstructor.builder()
            .key(CDStepDependencyUtils.getInfraKey(context))
            .providerPlanNodeId(infraNodeId)
            .outcomeExpression(OutcomeExpressionConstants.INFRASTRUCTURE.getName())
            .build();
    stepDependencyService.registerStepDependencyInstructor(instructor, context);
    return planNodeBuilder.build();
  }

  private PlanNode getInfraSectionNode(PlanNode infraStepNode, PlanNode envNode) {
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

  /** Method returns actual Infra object by resolving useFromStage if present. */
  private PipelineInfrastructure getActualInfraConfig(
      PipelineInfrastructure infrastructure, CreateExecutionPlanContext context) {
    if (infrastructure.getUseFromStage() != null) {
      if (infrastructure.getInfrastructureSpec() != null) {
        throw new InvalidArgumentsException("Infrastructure should not exist with UseFromStage.");
      }
      //  Add validation for not chaining of stages
      CDStage previousStage = PlanCreatorConfigUtils.getGivenDeploymentStageFromPipeline(
          context, infrastructure.getUseFromStage().getStage());
      if (previousStage != null) {
        DeploymentStage deploymentStage = (DeploymentStage) previousStage;
        return infrastructure.applyUseFromStage(deploymentStage.getDeployment().getInfrastructure());
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
