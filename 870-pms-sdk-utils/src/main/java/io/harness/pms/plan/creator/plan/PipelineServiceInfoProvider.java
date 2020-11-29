package io.harness.pms.plan.creator.plan;

import io.harness.pms.plan.creator.filters.FilterJsonCreator;

import java.util.List;

public interface PipelineServiceInfoProvider {
  String getServiceName();
  List<PartialPlanCreator<?>> getPlanCreators();
  List<FilterJsonCreator> getFilterJsonCreators();
}
