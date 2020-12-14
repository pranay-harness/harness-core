package io.harness.pms.sdk.core.plan.creation.creators;

import io.harness.pms.sdk.core.pipeline.filters.FilterJsonCreator;
import io.harness.pms.steps.StepInfo;

import java.util.List;

public interface PipelineServiceInfoProvider {
  List<PartialPlanCreator<?>> getPlanCreators();
  List<FilterJsonCreator> getFilterJsonCreators();
  List<StepInfo> getStepInfo();
}
