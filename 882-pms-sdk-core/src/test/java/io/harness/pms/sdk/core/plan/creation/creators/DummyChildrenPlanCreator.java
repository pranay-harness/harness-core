package io.harness.pms.sdk.core.plan.creation.creators;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.yaml.YAMLFieldNameConstants;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(HarnessTeam.PIPELINE)
public class DummyChildrenPlanCreator extends ChildrenPlanCreator<DummyChildrenPlanCreatorParam> {
  @Override
  public LinkedHashMap<String, PlanCreationResponse> createPlanForChildrenNodes(
      PlanCreationContext ctx, DummyChildrenPlanCreatorParam config) {
    return new LinkedHashMap<>();
  }

  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, DummyChildrenPlanCreatorParam config, List<String> childrenNodeIds) {
    return PlanNode.builder().build();
  }

  @Override
  public Class<DummyChildrenPlanCreatorParam> getFieldClass() {
    return DummyChildrenPlanCreatorParam.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    return Collections.singletonMap(YAMLFieldNameConstants.PIPELINE, Collections.singleton(PlanCreatorUtils.ANY_TYPE));
  }
}
