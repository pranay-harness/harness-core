package io.harness.cdng.pipeline.plancreators;

import static io.harness.cdng.executionplan.CDPlanCreatorType.ARTIFACT_FORK_PLAN_CREATOR;
import static io.harness.cdng.executionplan.CDPlanCreatorType.ARTIFACT_PLAN_CREATOR;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Collections.singletonList;

import com.google.inject.Inject;

import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.steps.ArtifactForkStep;
import io.harness.cdng.artifact.steps.ArtifactStepParameters;
import io.harness.cdng.artifact.steps.ArtifactStepParameters.ArtifactStepParametersBuilder;
import io.harness.cdng.executionplan.CDPlanNodeType;
import io.harness.cdng.service.ServiceConfig;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidArgumentsException;
import io.harness.executionplan.core.AbstractPlanCreatorWithChildren;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.ExecutionPlanCreator;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.service.ExecutionPlanCreatorHelper;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import io.harness.state.core.fork.ForkStepParameters;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ArtifactForkPlanCreator
    extends AbstractPlanCreatorWithChildren<ServiceConfig> implements SupportDefinedExecutorPlanCreator<ServiceConfig> {
  @Inject private ExecutionPlanCreatorHelper executionPlanCreatorHelper;

  @Override
  protected Map<String, List<CreateExecutionPlanResponse>> createPlanForChildren(
      ServiceConfig serviceConfig, CreateExecutionPlanContext context) {
    Map<String, List<CreateExecutionPlanResponse>> childrenPlanMap = new HashMap<>();
    final List<CreateExecutionPlanResponse> planForArtifacts = getPlanForArtifacts(context, serviceConfig);
    childrenPlanMap.put("ARTIFACTS", planForArtifacts);
    return childrenPlanMap;
  }

  @Override
  protected CreateExecutionPlanResponse createPlanForSelf(ServiceConfig serviceConfig,
      Map<String, List<CreateExecutionPlanResponse>> planForChildrenMap, CreateExecutionPlanContext context) {
    List<CreateExecutionPlanResponse> planForArtifacts = planForChildrenMap.get("ARTIFACTS");

    List<String> childNodeIds =
        planForArtifacts.stream().map(CreateExecutionPlanResponse::getStartingNodeId).collect(Collectors.toList());
    List<PlanNode> planNodes = getPlanNodes(planForArtifacts);

    final PlanNode artifactForkNode = prepareArtifactForkNode(childNodeIds);
    return CreateExecutionPlanResponse.builder()
        .planNode(artifactForkNode)
        .planNodes(planNodes)
        .startingNodeId(artifactForkNode.getUuid())
        .build();
  }

  private List<CreateExecutionPlanResponse> getPlanForArtifacts(
      CreateExecutionPlanContext context, ServiceConfig serviceConfig) {
    List<ArtifactStepParameters> artifactsWithCorrespondingOverrides =
        getArtifactsWithCorrespondingOverrides(serviceConfig);
    return artifactsWithCorrespondingOverrides.stream()
        .map(artifact -> getPlanCreatorForArtifact(artifact, context).createPlan(artifact, context))
        .collect(Collectors.toList());
  }

  private ExecutionPlanCreator<ArtifactStepParameters> getPlanCreatorForArtifact(
      ArtifactStepParameters artifactStepParameters, CreateExecutionPlanContext context) {
    return executionPlanCreatorHelper.getExecutionPlanCreator(ARTIFACT_PLAN_CREATOR.getName(), artifactStepParameters,
        context, "No execution plan creator found for artifact execution");
  }

  private PlanNode prepareArtifactForkNode(List<String> childNodeIds) {
    final String nodeId = generateUuid();

    final String artifactsIdentifier = "artifacts";
    return PlanNode.builder()
        .uuid(nodeId)
        .name(artifactsIdentifier)
        .identifier("artifacts")
        .stepType(ArtifactForkStep.STEP_TYPE)
        .stepParameters(ForkStepParameters.builder().parallelNodeIds(childNodeIds).build())
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

  private List<ArtifactStepParameters> getArtifactsWithCorrespondingOverrides(ServiceConfig serviceConfig) {
    Map<String, ArtifactStepParametersBuilder> artifactsMap = new HashMap<>();
    ArtifactListConfig artifacts = serviceConfig.getServiceSpec().getArtifacts();

    if (artifacts != null) {
      if (artifacts.getPrimary() == null) {
        throw new InvalidArgumentsException("Primary artifact cannot be null.");
      }
      // Add primary artifact.
      artifactsMap.put(
          artifacts.getPrimary().getArtifactType(), ArtifactStepParameters.builder().artifact(artifacts.getPrimary()));

      // Add sidecars.
      if (EmptyPredicate.isNotEmpty(artifacts.getSidecars())) {
        artifacts.getSidecars().forEach(sidecarArtifactWrapper -> {
          String key =
              sidecarArtifactWrapper.getArtifact().getArtifactType() + "." + sidecarArtifactWrapper.getIdentifier();
          if (artifactsMap.containsKey(key)) {
            throw new InvalidArgumentsException("Same identifier sidecar is occurring multiple times in the list.");
          }
          artifactsMap.put(key, ArtifactStepParameters.builder().artifact(sidecarArtifactWrapper.getArtifact()));
        });
      }
    }

    // Add Stage Overrides.
    ArtifactListConfig stageOverrides = serviceConfig.getStageOverrides().getArtifacts();
    if (stageOverrides != null) {
      if (stageOverrides.getPrimary() != null) {
        artifactsMap.get(stageOverrides.getPrimary().getArtifactType())
            .artifactStageOverride(stageOverrides.getPrimary());
      }

      if (EmptyPredicate.isNotEmpty(stageOverrides.getSidecars())) {
        stageOverrides.getSidecars().forEach(sidecar -> {
          String key = sidecar.getArtifact().getArtifactType() + "." + sidecar.getIdentifier();
          artifactsMap.get(key).artifactStageOverride(sidecar.getArtifact());
        });
      }
    }

    List<ArtifactStepParameters> mappedArtifacts = new LinkedList<>();
    artifactsMap.forEach((key, value) -> mappedArtifacts.add(value.build()));
    return mappedArtifacts;
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType())
        && searchContext.getObjectToPlan() instanceof ServiceConfig;
  }

  @Override
  public List<String> getSupportedTypes() {
    return singletonList(ARTIFACT_FORK_PLAN_CREATOR.getName());
  }

  @Override
  protected String getPlanNodeType(ServiceConfig input) {
    return CDPlanNodeType.ARTIFACTS.name();
  }
}
