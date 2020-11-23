package io.harness.ci.plan.creator;

import io.harness.pms.sdk.creator.PartialPlanCreator;
import io.harness.pms.sdk.creator.PlanCreatorProvider;

import java.util.LinkedList;
import java.util.List;

public class CIPlanCreatorProvider implements PlanCreatorProvider {
  @Override
  public String getServiceName() {
    return "CI";
  }

  @Override
  public List<PartialPlanCreator<?>> getPlanCreators() {
    return new LinkedList<>();
  }
}
