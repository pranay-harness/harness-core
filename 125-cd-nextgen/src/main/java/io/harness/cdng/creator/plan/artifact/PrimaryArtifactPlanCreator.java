package io.harness.cdng.creator.plan.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.yaml.PrimaryArtifact;
import io.harness.cdng.artifact.steps.ArtifactStep;
import io.harness.cdng.artifact.steps.ArtifactStepParameters;
import io.harness.cdng.creator.plan.PlanCreatorConstants;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.delegate.task.artifacts.ArtifactSourceConstants;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.PartialPlanCreator;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.CDC)
public class PrimaryArtifactPlanCreator implements PartialPlanCreator<PrimaryArtifact> {
  @Inject KryoSerializer kryoSerializer;
  @Override
  public Class<PrimaryArtifact> getFieldClass() {
    return PrimaryArtifact.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(YamlTypes.PRIMARY_ARTIFACT,
        new HashSet<>(Arrays.asList(ArtifactSourceConstants.DOCKER_REGISTRY_NAME, ArtifactSourceConstants.ECR_NAME,
            ArtifactSourceConstants.GCR_NAME)));
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, PrimaryArtifact artifactInfo) {
    String primaryId = (String) kryoSerializer.asInflatedObject(
        ctx.getDependency().getMetadataMap().get(YamlTypes.UUID).toByteArray());
    StepParameters stepParameters = (StepParameters) kryoSerializer.asInflatedObject(
        ctx.getDependency().getMetadataMap().get(YamlTypes.PRIMARY_STEP_PARAMETERS).toByteArray());

    PlanNode artifactNode =
        PlanNode.builder()
            .uuid(primaryId)
            .stepType(ArtifactStep.STEP_TYPE)
            .name(PlanCreatorConstants.ARTIFACT_NODE_NAME)
            .identifier(ctx.getCurrentField().getNode().getIdentifier())
            .stepParameters(stepParameters)
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                    .build())
            .skipExpressionChain(false)
            .build();
    return PlanCreationResponse.builder().planNode(artifactNode).build();
  }
}