package io.harness.pms.sdk.creator;

import io.harness.pms.sdk.creator.filters.FilterJsonCreator;

import java.util.List;

public interface PipelineServiceInfoProvider {
  String getServiceName();
  List<PartialPlanCreator<?>> getPlanCreators();
  List<FilterJsonCreator> getFilterJsonCreators();
}
