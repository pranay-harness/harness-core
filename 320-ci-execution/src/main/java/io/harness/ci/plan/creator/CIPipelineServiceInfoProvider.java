package io.harness.ci.plan.creator;

import io.harness.pms.plan.creator.filters.FilterJsonCreator;
import io.harness.pms.plan.creator.plan.PartialPlanCreator;
import io.harness.pms.plan.creator.plan.PipelineServiceInfoProvider;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CIPipelineServiceInfoProvider implements PipelineServiceInfoProvider {
  @Override
  public String getServiceName() {
    return "CI";
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
