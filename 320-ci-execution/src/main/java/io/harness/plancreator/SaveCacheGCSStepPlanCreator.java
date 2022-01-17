package io.harness.plancreator;

import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.nodes.SaveCacheGCSNode;
import io.harness.ci.plan.creator.step.CIPMSStepPlanCreatorV2;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

public class SaveCacheGCSStepPlanCreator extends CIPMSStepPlanCreatorV2<SaveCacheGCSNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(CIStepInfoType.SAVE_CACHE_GCS.getDisplayName());
  }

  @Override
  public Class<SaveCacheGCSNode> getFieldClass() {
    return SaveCacheGCSNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(PlanCreationContext ctx, SaveCacheGCSNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}
