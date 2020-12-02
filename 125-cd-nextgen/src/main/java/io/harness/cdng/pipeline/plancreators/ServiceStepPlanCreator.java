package io.harness.cdng.pipeline.plancreators;

import static io.harness.cdng.executionplan.CDPlanCreatorType.SERVICE_PLAN_CREATOR;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static java.util.Collections.singletonList;

import io.harness.cdng.executionplan.CDPlanNodeType;
import io.harness.cdng.executionplan.utils.PlanCreatorConfigUtils;
import io.harness.cdng.pipeline.CDStage;
import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.service.beans.ServiceUseFromStage;
import io.harness.cdng.service.steps.ServiceStep;
import io.harness.cdng.service.steps.ServiceStepParameters;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.cdng.stepsdependency.utils.CDStepDependencyUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidArgumentsException;
import io.harness.executionplan.core.AbstractPlanCreatorWithChildren;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.ExecutionPlanCreatorResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.plancreator.beans.PlanCreatorConstants;
import io.harness.executionplan.stepsdependency.StepDependencyService;
import io.harness.executionplan.stepsdependency.instructors.OutcomeRefStepDependencyInstructor;
import io.harness.facilitator.OrchestrationFacilitatorType;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.PlanNode.PlanNodeBuilder;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class ServiceStepPlanCreator
    extends AbstractPlanCreatorWithChildren<ServiceConfig> implements SupportDefinedExecutorPlanCreator<ServiceConfig> {
  @Inject private StepDependencyService stepDependencyService;

  @Override
  public Map<String, List<ExecutionPlanCreatorResponse>> createPlanForChildren(
      ServiceConfig serviceConfig, ExecutionPlanCreationContext context) {
    return null;
  }

  @Override
  public ExecutionPlanCreatorResponse createPlanForSelf(ServiceConfig serviceConfig,
      Map<String, List<ExecutionPlanCreatorResponse>> planForChildrenMap, ExecutionPlanCreationContext context) {
    ServiceConfig actualServiceConfig = getActualServiceConfig(serviceConfig, context);
    final PlanNode serviceExecutionNode = prepareServiceNode(actualServiceConfig, context);
    return ExecutionPlanCreatorResponse.builder()
        .planNode(serviceExecutionNode)
        .startingNodeId(serviceExecutionNode.getUuid())
        .build();
  }

  private PlanNode prepareServiceNode(ServiceConfig serviceConfig, ExecutionPlanCreationContext context) {
    final String serviceNodeUid = generateUuid();

    if (!serviceConfig.getName().isExpression() && EmptyPredicate.isEmpty(serviceConfig.getName().getValue())) {
      serviceConfig.setName(serviceConfig.getIdentifier());
    }

    ServiceConfig serviceOverrides = null;
    if (serviceConfig.getUseFromStage() != null) {
      ServiceUseFromStage.Overrides overrides = serviceConfig.getUseFromStage().getOverrides();
      if (overrides != null) {
        serviceOverrides =
            ServiceConfig.builder().name(overrides.getName()).description(overrides.getDescription()).build();
      }
    }

    final String serviceIdentifier = "service";
    PlanNodeBuilder planNodeBuilder =
        PlanNode.builder()
            .uuid(serviceNodeUid)
            .name(PlanCreatorConstants.SERVICE_NODE_NAME)
            .identifier(serviceIdentifier)
            .stepType(ServiceStep.STEP_TYPE)
            .stepParameters(
                ServiceStepParameters.builder().service(serviceConfig).serviceOverrides(serviceOverrides).build())
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK_CHAIN_V3).build())
                    .build());

    // Adding dependency provider.
    OutcomeRefStepDependencyInstructor instructor = OutcomeRefStepDependencyInstructor.builder()
                                                        .key(CDStepDependencyUtils.getServiceKey(context))
                                                        .providerPlanNodeId(serviceNodeUid)
                                                        .outcomeExpression(OutcomeExpressionConstants.SERVICE)
                                                        .build();
    stepDependencyService.registerStepDependencyInstructor(instructor, context);
    return planNodeBuilder.build();
  }

  /** Method returns actual Service object by resolving useFromStage if present. */
  private ServiceConfig getActualServiceConfig(ServiceConfig serviceConfig, ExecutionPlanCreationContext context) {
    if (serviceConfig.getUseFromStage() != null) {
      if (serviceConfig.getServiceDefinition() != null) {
        throw new InvalidArgumentsException("KubernetesServiceSpec should not exist with UseFromStage.");
      }
      //  Add validation for not chaining of stages
      CDStage previousStage = PlanCreatorConfigUtils.getGivenDeploymentStageFromPipeline(
          context, serviceConfig.getUseFromStage().getStage().getValue());
      if (previousStage != null) {
        DeploymentStage deploymentStage = (DeploymentStage) previousStage;
        return serviceConfig.applyUseFromStage(deploymentStage.getService());
      } else {
        throw new InvalidArgumentsException("Stage identifier given in useFromStage doesn't exist.");
      }
    }
    return serviceConfig;
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType())
        && searchContext.getObjectToPlan() instanceof ServiceConfig;
  }

  @Override
  public List<String> getSupportedTypes() {
    return singletonList(SERVICE_PLAN_CREATOR.getName());
  }

  @Override
  public String getPlanNodeType(ServiceConfig input) {
    return CDPlanNodeType.SERVICE.name();
  }
}
