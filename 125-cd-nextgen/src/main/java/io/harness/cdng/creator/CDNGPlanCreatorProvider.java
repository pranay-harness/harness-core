package io.harness.cdng.creator;

import io.harness.pms.sdk.creator.PartialPlanCreator;
import io.harness.pms.sdk.creator.PlanCreatorProvider;

import java.util.LinkedList;
import java.util.List;

public class CDNGPlanCreatorProvider implements PlanCreatorProvider {
  @Override
  public String getServiceName() {
    return "cd";
  }

  @Override
  public List<PartialPlanCreator<?>> getPlanCreators() {
    return new LinkedList<>();
  }
}
