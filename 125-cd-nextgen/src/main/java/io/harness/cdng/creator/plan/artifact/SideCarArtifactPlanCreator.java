/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.artifact;

import com.google.inject.Inject;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.yaml.PrimaryArtifact;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifactWrapper;
import io.harness.cdng.artifact.steps.ArtifactStep;
import io.harness.cdng.creator.plan.PlanCreatorConstants;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.delegate.task.artifacts.ArtifactSourceConstants;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.sdk.core.plan.creation.creators.PartialPlanCreator;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.serializer.KryoSerializer;

import java.util.*;

@OwnedBy(HarnessTeam.CDC)
public class SideCarArtifactPlanCreator implements PartialPlanCreator<SidecarArtifactWrapper> {
  @Inject KryoSerializer kryoSerializer;
  @Override
  public Class<SidecarArtifactWrapper> getFieldClass() {
    return SidecarArtifactWrapper.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(YamlTypes.SIDECAR_ARTIFACT_CONFIG,
        new HashSet<>(Arrays.asList(ArtifactSourceConstants.DOCKER_REGISTRY_NAME, ArtifactSourceConstants.ECR_NAME,
            ArtifactSourceConstants.GCR_NAME)));
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, SidecarArtifactWrapper artifactInfo) {
    String sideCarArtifactId = (String) kryoSerializer.asInflatedObject(
        ctx.getDependency().getMetadataMap().get(YamlTypes.UUID).toByteArray());
    StepParameters stepParameters = (StepParameters) kryoSerializer.asInflatedObject(
        ctx.getDependency().getMetadataMap().get(PlanCreatorConstants.SIDECARS_PARAMETERS_MAP).toByteArray());

    PlanNode artifactNode =
        PlanNode.builder()
            .uuid(sideCarArtifactId)
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
