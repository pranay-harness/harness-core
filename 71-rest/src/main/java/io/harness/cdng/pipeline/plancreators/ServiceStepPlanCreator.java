package io.harness.cdng.pipeline.plancreators;

import static io.harness.cdng.executionplan.CDPlanCreatorType.ARTIFACT_PLAN_CREATOR;
import static io.harness.cdng.executionplan.CDPlanCreatorType.MANIFEST_PLAN_CREATOR;
import static io.harness.cdng.executionplan.CDPlanCreatorType.SERVICE_PLAN_CREATOR;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.artifact.bean.ArtifactConfigWrapper;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.service.Service;
import io.harness.cdng.service.steps.ServiceStep;
import io.harness.cdng.service.steps.ServiceStepParameters;
import io.harness.data.structure.EmptyPredicate;
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
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class ServiceStepPlanCreator implements SupportDefinedExecutorPlanCreator<Service> {
  @Inject private ExecutionPlanCreatorHelper executionPlanCreatorHelper;

  @Override
  public CreateExecutionPlanResponse createPlan(Service service, CreateExecutionPlanContext context) {
    final List<CreateExecutionPlanResponse> planForArtifacts =
        getPlanForArtifacts(context, service.getServiceSpec().getArtifacts());

    final CreateExecutionPlanResponse planForManifests = getPlanForManifests(context, service);

    // Add artifactNodes and ManifestNode as children
    List<String> childNodeIds =
        planForArtifacts.stream().map(CreateExecutionPlanResponse::getStartingNodeId).collect(Collectors.toList());
    childNodeIds.add(planForManifests.getStartingNodeId());

    List<PlanNode> planNodes = getPlanNodes(planForArtifacts);
    planNodes.addAll(planForManifests.getPlanNodes());

    final PlanNode serviceExecutionNode = prepareServiceNode(service, childNodeIds);
    return CreateExecutionPlanResponse.builder()
        .planNode(serviceExecutionNode)
        .planNodes(planNodes)
        .startingNodeId(serviceExecutionNode.getUuid())
        .build();
  }

  private PlanNode prepareServiceNode(Service service, List<String> childNodeIds) {
    final String serviceNodeUid = generateUuid();

    service.setDisplayName(StringUtils.defaultIfEmpty(service.getDisplayName(), service.getIdentifier()));

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

  private ExecutionPlanCreator<ArtifactConfigWrapper> getPlanCreatorForArtifact(
      CreateExecutionPlanContext context, ArtifactConfigWrapper artifactConfigWrapper) {
    return executionPlanCreatorHelper.getExecutionPlanCreator(ARTIFACT_PLAN_CREATOR.getName(), artifactConfigWrapper,
        context, "No execution plan creator found for artifact execution");
  }

  private CreateExecutionPlanResponse getPlanForManifests(CreateExecutionPlanContext context, Service service) {
    final ExecutionPlanCreator<Service> executionPlanCreator = executionPlanCreatorHelper.getExecutionPlanCreator(
        MANIFEST_PLAN_CREATOR.getName(), service, context, "No execution plan creator found for Manifests execution");

    return executionPlanCreator.createPlan(service, context);
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
