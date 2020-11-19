package io.harness.cdng.creator;

import io.harness.pms.sdk.creator.PartialPlanCreator;
import io.harness.pms.sdk.creator.PlanCreatorProvider;

import java.util.LinkedList;
import java.util.List;

public class NGPlanCreatorProvider implements PlanCreatorProvider {
  @Override
  public String getServiceName() {
    return "CD";
  }

  @Override
  public List<PartialPlanCreator<?>> getPlanCreators() {
    return new LinkedList<>();
  }
}
