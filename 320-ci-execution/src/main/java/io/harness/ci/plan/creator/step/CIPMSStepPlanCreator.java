package io.harness.ci.plan.creator.step;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.GenericStepPMSPlanCreator;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.execution.utils.RunInfoUtils;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepOutcomeGroup;
import io.harness.timeout.contracts.TimeoutObtainment;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutParameters;
import io.harness.timeout.trackers.absolute.AbsoluteTimeoutTrackerFactory;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.Set;

@OwnedBy(HarnessTeam.CI)
public class CIPMSStepPlanCreator extends GenericStepPMSPlanCreator {
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet("SaveCacheS3", "Test", "RunTests", "SaveCache", "liteEngineTask", "GitClone",
        "BuildAndPushGCR", "BuildAndPushECR", "BuildAndPushDockerRegistry", "Cleanup", "Plugin", "PublishArtifacts",
        "RestoreCacheGCS", "RestoreCacheS3", "RestoreCache", "SaveCacheGCS", "Run", "S3Upload", "GCSUpload",
        "ArtifactoryUpload");
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, StepElementConfig stepElement) {
    PlanNode stepPlanNode =
        PlanNode.builder()
            .uuid(ctx.getCurrentField().getNode().getUuid())
            .name(getName(stepElement))
            .identifier(stepElement.getIdentifier())
            .stepType(stepElement.getStepSpecType().getStepType())
            .group(StepOutcomeGroup.STEP.name())
            .stepParameters(stepElement.getStepSpecType().getStepParameters())
            .whenCondition(RunInfoUtils.getRunCondition(stepElement.getWhen(), false))
            .facilitatorObtainment(FacilitatorObtainment.newBuilder()
                                       .setType(FacilitatorType.newBuilder()
                                                    .setType(stepElement.getStepSpecType().getFacilitatorType())
                                                    .build())
                                       .build())
            .adviserObtainments(getAdviserObtainmentFromMetaData(ctx.getCurrentField()))
            .timeoutObtainment(
                TimeoutObtainment.newBuilder()
                    .setDimension(AbsoluteTimeoutTrackerFactory.DIMENSION)
                    .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                        AbsoluteTimeoutParameters.builder().timeoutMillis(getTimeoutInMillis(stepElement)).build())))
                    .build())
            .build();
    return PlanCreationResponse.builder().node(stepPlanNode.getUuid(), stepPlanNode).build();
  }
}