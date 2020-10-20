package io.harness.pipeline.plan.scratch.cv;

import io.harness.pipeline.plan.scratch.common.creator.PartialPlanCreator;
import io.harness.pipeline.plan.scratch.lib.creator.PipelinePlanCreator;
import io.harness.pipeline.plan.scratch.common.creator.PlanCreatorProvider;

import java.util.ArrayList;
import java.util.List;

public class CVPlanCreatorProvider implements PlanCreatorProvider {
  @Override
  public List<PartialPlanCreator> getPlanCreators() {
    List<PartialPlanCreator> planCreators = new ArrayList<>();
    planCreators.add(new PipelinePlanCreator());
    planCreators.add(new CVStepPlanCreator());
    return planCreators;
  }
}
