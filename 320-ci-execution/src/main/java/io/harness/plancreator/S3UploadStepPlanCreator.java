package io.harness.plancreator;

import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.nodes.S3UploadNode;
import io.harness.ci.plan.creator.step.CIPMSStepPlanCreatorV2;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class S3UploadStepPlanCreator extends CIPMSStepPlanCreatorV2<S3UploadNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(CIStepInfoType.UPLOAD_S3.getDisplayName());
  }

  @Override
  public Class<S3UploadNode> getFieldClass() {
    return S3UploadNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, S3UploadNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
