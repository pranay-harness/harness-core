package io.harness.pms.sample.cd.creator;

import io.harness.pms.sdk.creator.PartialPlanCreator;
import io.harness.pms.sdk.creator.PipelinePlanCreator;
import io.harness.pms.sdk.creator.PlanCreatorProvider;

import java.util.ArrayList;
import java.util.List;

public class CdPlanCreatorProvider implements PlanCreatorProvider {
  @Override
  public String getServiceName() {
    return "cd";
  }

  @Override
  public List<PartialPlanCreator> getPlanCreators() {
    List<PartialPlanCreator> planCreators = new ArrayList<>();
    planCreators.add(new PipelinePlanCreator());
    planCreators.add(new DeploymentStagePlanCreator());
    planCreators.add(new CdStepPlanCreator());
    return planCreators;
  }
}
