package io.harness.plancreator;

import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.nodes.SaveCacheS3Node;
import io.harness.ci.plan.creator.step.CIPMSStepPlanCreatorV2;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class SaveCacheS3StepPlanCreator extends CIPMSStepPlanCreatorV2<SaveCacheS3Node> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(CIStepInfoType.SAVE_CACHE_S3.getDisplayName());
  }

  @Override
  public Class<SaveCacheS3Node> getFieldClass() {
    return SaveCacheS3Node.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, SaveCacheS3Node stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
