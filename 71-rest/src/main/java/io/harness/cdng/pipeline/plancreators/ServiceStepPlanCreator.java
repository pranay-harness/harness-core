package io.harness.cdng.pipeline.plancreators;

import static io.harness.cdng.executionplan.CDPlanCreatorType.ARTIFACT_PLAN_CREATOR;
import static io.harness.cdng.executionplan.CDPlanCreatorType.MANIFEST_PLAN_CREATOR;
import static io.harness.cdng.executionplan.CDPlanCreatorType.SERVICE_PLAN_CREATOR;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Collections.singletonList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.artifact.bean.ArtifactConfigWrapper;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.executionplan.CDPlanNodeType;
import io.harness.cdng.executionplan.utils.PlanCreatorConfigUtils;
import io.harness.cdng.pipeline.CDStage;
import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.cdng.service.ServiceConfig;
import io.harness.cdng.service.steps.ServiceStep;
import io.harness.cdng.service.steps.ServiceStepParameters;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.cdng.stepsdependency.utils.CDStepDependencyUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.executionplan.core.AbstractPlanCreatorWithChildren;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.executionplan.stepsdependency.StepDependencyService;
import io.harness.executionplan.stepsdependency.instructors.OutcomeRefStepDependencyInstructor;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import io.harness.plan.PlanNode.PlanNodeBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class ServiceStepPlanCreator
    extends AbstractPlanCreatorWithChildren<ServiceConfig> implements SupportDefinedExecutorPlanCreator<ServiceConfig> {
  @Inject private ExecutionPlanCreatorHelper executionPlanCreatorHelper;
  @Inject private StepDependencyService stepDependencyService;

  @Override
  public Map<String, List<CreateExecutionPlanResponse>> createPlanForChildren(
      ServiceConfig serviceConfig, CreateExecutionPlanContext context) {
    Map<String, List<CreateExecutionPlanResponse>> childrenPlanMap = new HashMap<>();
    ServiceConfig actualServiceConfig = getActualServiceConfig(serviceConfig, context);
    final List<CreateExecutionPlanResponse> planForArtifacts =
        getPlanForArtifacts(context, actualServiceConfig.getServiceSpec().getArtifacts());
    final CreateExecutionPlanResponse planForManifests = getPlanForManifests(context, actualServiceConfig);
    childrenPlanMap.put("ARTIFACTS", planForArtifacts);
    childrenPlanMap.put("MANIFESTS", singletonList(planForManifests));
    return childrenPlanMap;
  }

  @Override
  public CreateExecutionPlanResponse createPlanForSelf(ServiceConfig serviceConfig,
      Map<String, List<CreateExecutionPlanResponse>> planForChildrenMap, CreateExecutionPlanContext context) {
    ServiceConfig actualServiceConfig = getActualServiceConfig(serviceConfig, context);
    List<CreateExecutionPlanResponse> planForArtifacts = planForChildrenMap.get("ARTIFACTS");
    CreateExecutionPlanResponse planForManifests = planForChildrenMap.get("MANIFESTS").get(0);

    // Add artifactNodes and ManifestNode as children
    List<String> childNodeIds =
        planForArtifacts.stream().map(CreateExecutionPlanResponse::getStartingNodeId).collect(Collectors.toList());
    childNodeIds.add(planForManifests.getStartingNodeId());

    List<PlanNode> planNodes = getPlanNodes(planForArtifacts);
    planNodes.addAll(planForManifests.getPlanNodes());

    final PlanNode serviceExecutionNode = prepareServiceNode(actualServiceConfig, childNodeIds, context);
    return CreateExecutionPlanResponse.builder()
        .planNode(serviceExecutionNode)
        .planNodes(planNodes)
        .startingNodeId(serviceExecutionNode.getUuid())
        .build();
  }

  private PlanNode prepareServiceNode(
      ServiceConfig serviceConfig, List<String> childNodeIds, CreateExecutionPlanContext context) {
    final String serviceNodeUid = generateUuid();

    serviceConfig.setDisplayName(
        StringUtils.defaultIfEmpty(serviceConfig.getDisplayName(), serviceConfig.getIdentifier()));

    PlanNodeBuilder planNodeBuilder =
        PlanNode.builder()
            .uuid(serviceNodeUid)
            .name(serviceConfig.getDisplayName())
            .identifier(serviceConfig.getIdentifier())
            .stepType(ServiceStep.STEP_TYPE)
            .stepParameters(
                ServiceStepParameters.builder().parallelNodeIds(childNodeIds).service(serviceConfig).build())
            .facilitatorObtainment(FacilitatorObtainment.builder()
                                       .type(FacilitatorType.builder().type(FacilitatorType.CHILDREN).build())
                                       .build());

    // Adding dependency provider.
    OutcomeRefStepDependencyInstructor instructor = OutcomeRefStepDependencyInstructor.builder()
                                                        .key(CDStepDependencyUtils.getServiceKey(context))
                                                        .providerPlanNodeId(serviceNodeUid)
                                                        .outcomeExpression(OutcomeExpressionConstants.SERVICE.getName())
                                                        .build();
    stepDependencyService.registerStepDependencyInstructor(instructor, context);
    return planNodeBuilder.build();
  }

  @NotNull
  private List<PlanNode> getPlanNodes(List<CreateExecutionPlanResponse> planForChild) {
    return planForChild.stream()
        .flatMap(createExecutionPlanResponse -> createExecutionPlanResponse.getPlanNodes().stream())
        .collect(Collectors.toList());
  }

  private List<CreateExecutionPlanResponse> getPlanForArtifacts(
      CreateExecutionPlanContext context, ArtifactListConfig artifactListConfig) {
    List<CreateExecutionPlanResponse> planResponseList = new ArrayList<>();
    planResponseList.add(getPlanCreatorForArtifact(context, artifactListConfig.getPrimary())
                             .createPlan(artifactListConfig.getPrimary(), context));
    if (EmptyPredicate.isNotEmpty(artifactListConfig.getSidecars())) {
      List<CreateExecutionPlanResponse> planResponses =
          artifactListConfig.getSidecars()
              .stream()
              .map(sidecarArtifact
                  -> getPlanCreatorForArtifact(context, sidecarArtifact.getArtifact())
                         .createPlan(sidecarArtifact.getArtifact(), context))
              .collect(Collectors.toList());
      planResponseList.addAll(planResponses);
    }
    return planResponseList;
  }

  /** Method returns actual Service object by resolving useFromStage if present. */
  private ServiceConfig getActualServiceConfig(ServiceConfig serviceConfig, CreateExecutionPlanContext context) {
    if (serviceConfig.getUseFromStage() != null) {
      if (serviceConfig.getServiceSpec() != null) {
        throw new IllegalArgumentException("ServiceSpec should not exist with UseFromStage.");
      }
      //  Add validation for not chaining of stages
      CDStage previousStage = PlanCreatorConfigUtils.getGivenDeploymentStageFromPipeline(
          context, serviceConfig.getUseFromStage().getStage());
      if (previousStage != null) {
        DeploymentStage deploymentStage = (DeploymentStage) previousStage;
        return deploymentStage.getDeployment().getService().mergeNonNullProperties(serviceConfig);
      } else {
        throw new IllegalArgumentException("Stage identifier given in useFromStage doesn't exist.");
      }
    }
    return serviceConfig;
  }

  private ExecutionPlanCreator<ArtifactConfigWrapper> getPlanCreatorForArtifact(
      CreateExecutionPlanContext context, ArtifactConfigWrapper artifactConfigWrapper) {
    return executionPlanCreatorHelper.getExecutionPlanCreator(ARTIFACT_PLAN_CREATOR.getName(), artifactConfigWrapper,
        context, "No execution plan creator found for artifact execution");
  }

  private CreateExecutionPlanResponse getPlanForManifests(
      CreateExecutionPlanContext context, ServiceConfig serviceConfig) {
    final ExecutionPlanCreator<ServiceConfig> executionPlanCreator =
        executionPlanCreatorHelper.getExecutionPlanCreator(MANIFEST_PLAN_CREATOR.getName(), serviceConfig, context,
            "No execution plan creator found for Manifests execution");

    return executionPlanCreator.createPlan(serviceConfig, context);
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
