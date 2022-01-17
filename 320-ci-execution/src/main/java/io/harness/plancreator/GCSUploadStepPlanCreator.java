package io.harness.plancreator;

import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.nodes.GCSUploadNode;
import io.harness.ci.plan.creator.step.CIPMSStepPlanCreatorV2;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class GCSUploadStepPlanCreator extends CIPMSStepPlanCreatorV2<GCSUploadNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(CIStepInfoType.UPLOAD_GCS.getDisplayName());
  }

  @Override
  public Class<GCSUploadNode> getFieldClass() {
    return GCSUploadNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, GCSUploadNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
