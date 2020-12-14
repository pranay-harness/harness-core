package io.harness.ci.plan.creator;

import io.harness.pms.sdk.core.pipeline.filters.FilterJsonCreator;
import io.harness.pms.sdk.core.plan.creation.creators.PartialPlanCreator;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;
import io.harness.pms.steps.StepInfo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CIPipelineServiceInfoProvider implements PipelineServiceInfoProvider {
  @Override
  public List<PartialPlanCreator<?>> getPlanCreators() {
    return new LinkedList<>();
  }

  @Override
  public List<FilterJsonCreator> getFilterJsonCreators() {
    return new ArrayList<>();
  }

  @Override
  public List<StepInfo> getStepInfo() {
    return new ArrayList<>();
  }
}
