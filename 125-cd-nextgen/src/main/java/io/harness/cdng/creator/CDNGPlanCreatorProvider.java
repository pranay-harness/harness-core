package io.harness.cdng.creator;

import io.harness.pms.plan.creator.filters.FilterJsonCreator;
import io.harness.pms.plan.creator.plan.PartialPlanCreator;
import io.harness.pms.plan.creator.plan.PipelineServiceInfoProvider;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CDNGPlanCreatorProvider implements PipelineServiceInfoProvider {
  @Override
  public String getServiceName() {
    return "cd";
  }

  @Override
  public List<PartialPlanCreator<?>> getPlanCreators() {
    return new LinkedList<>();
  }

  @Override
  public List<FilterJsonCreator> getFilterJsonCreators() {
    return new ArrayList<>();
  }
}
