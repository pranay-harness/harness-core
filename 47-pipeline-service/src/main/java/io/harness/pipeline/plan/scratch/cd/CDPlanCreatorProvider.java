package io.harness.pipeline.plan.scratch.cd;

import io.harness.pipeline.plan.scratch.common.creator.PartialPlanCreator;
import io.harness.pipeline.plan.scratch.common.creator.PlanCreatorProvider;
import io.harness.pipeline.plan.scratch.lib.creator.PipelinePlanCreator;

import java.util.ArrayList;
import java.util.List;

public class CDPlanCreatorProvider implements PlanCreatorProvider {
  @Override
  public List<PartialPlanCreator> getPlanCreators() {
    List<PartialPlanCreator> planCreators = new ArrayList<>();
    planCreators.add(new PipelinePlanCreator());
    planCreators.add(new DeploymentStagePlanCreator());
    planCreators.add(new CDStepPlanCreator());
    return planCreators;
  }
}
