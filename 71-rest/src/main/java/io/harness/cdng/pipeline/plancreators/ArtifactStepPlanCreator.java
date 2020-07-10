package io.harness.cdng.pipeline.plancreators;

import static io.harness.cdng.executionplan.CDPlanCreatorType.ARTIFACT_PLAN_CREATOR;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import com.google.inject.Singleton;

import io.harness.cdng.artifact.steps.ArtifactStep;
import io.harness.cdng.artifact.steps.ArtifactStepParameters;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.core.CreateExecutionPlanResponse;
import io.harness.executionplan.core.PlanCreatorSearchContext;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Singleton
@Slf4j
public class ArtifactStepPlanCreator implements SupportDefinedExecutorPlanCreator<ArtifactStepParameters> {
  @Override
  public CreateExecutionPlanResponse createPlan(
      ArtifactStepParameters artifactStepParameters, CreateExecutionPlanContext context) {
    final PlanNode artifactExecutionNode = prepareArtifactStepExecutionNode(artifactStepParameters);

    return CreateExecutionPlanResponse.builder()
        .planNode(artifactExecutionNode)
        .startingNodeId(artifactExecutionNode.getUuid())
        .build();
  }

  private PlanNode prepareArtifactStepExecutionNode(ArtifactStepParameters artifactStepParameters) {
    final String artifactStepUid = generateUuid();
    String artifactNodeIdentifier;
    if (artifactStepParameters.getArtifact() != null) {
      artifactNodeIdentifier = ArtifactUtils.getArtifactKey(artifactStepParameters.getArtifact());
    } else if (artifactStepParameters.getArtifactOverrideSet() != null) {
      artifactNodeIdentifier = ArtifactUtils.getArtifactKey(artifactStepParameters.getArtifactOverrideSet());
    } else {
      artifactNodeIdentifier = ArtifactUtils.getArtifactKey(artifactStepParameters.getArtifactStageOverride());
    }

    return PlanNode.builder()
        .uuid(artifactStepUid)
        .name(artifactNodeIdentifier)
        .identifier(artifactNodeIdentifier)
        .stepType(ArtifactStep.STEP_TYPE)
        .stepParameters(artifactStepParameters)
        .facilitatorObtainment(
            FacilitatorObtainment.builder().type(FacilitatorType.builder().type(FacilitatorType.TASK).build()).build())
        .build();
  }

  @Override
  public boolean supports(PlanCreatorSearchContext<?> searchContext) {
    return getSupportedTypes().contains(searchContext.getType())
        && searchContext.getObjectToPlan() instanceof ArtifactStepParameters;
  }

  @Override
  public List<String> getSupportedTypes() {
    return Collections.singletonList(ARTIFACT_PLAN_CREATOR.getName());
  }
}
