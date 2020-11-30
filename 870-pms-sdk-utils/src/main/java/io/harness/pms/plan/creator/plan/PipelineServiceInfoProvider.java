package io.harness.pms.plan.creator.plan;

import io.harness.pms.plan.creator.filters.FilterJsonCreator;
import io.harness.pms.steps.StepInfo;

import java.util.List;

public interface PipelineServiceInfoProvider {
  String getServiceName();
  List<PartialPlanCreator<?>> getPlanCreators();
  List<FilterJsonCreator> getFilterJsonCreators();
  List<StepInfo> getStepInfo();
}
