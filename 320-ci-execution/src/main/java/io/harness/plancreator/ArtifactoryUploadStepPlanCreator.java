package io.harness.plancreator;

import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.nodes.ArtifactoryUploadNode;
import io.harness.ci.plan.creator.step.CIPMSStepPlanCreatorV2;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class ArtifactoryUploadStepPlanCreator extends CIPMSStepPlanCreatorV2<ArtifactoryUploadNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(CIStepInfoType.UPLOAD_ARTIFACTORY.getDisplayName());
  }

  @Override
  public Class<ArtifactoryUploadNode> getFieldClass() {
    return ArtifactoryUploadNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, ArtifactoryUploadNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
