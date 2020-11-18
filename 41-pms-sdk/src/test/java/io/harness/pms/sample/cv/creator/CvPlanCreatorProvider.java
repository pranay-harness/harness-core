package io.harness.pms.sample.cv.creator;

import io.harness.pms.sdk.creator.PartialPlanCreator;
import io.harness.pms.sdk.creator.PipelinePlanCreator;
import io.harness.pms.sdk.creator.PlanCreatorProvider;

import java.util.ArrayList;
import java.util.List;

public class CvPlanCreatorProvider implements PlanCreatorProvider {
  @Override
  public String getServiceName() {
    return "cv";
  }

  @Override
  public List<PartialPlanCreator<?>> getPlanCreators() {
    List<PartialPlanCreator<?>> planCreators = new ArrayList<>();
    planCreators.add(new PipelinePlanCreator());
    planCreators.add(new CvStepPlanCreator());
    return planCreators;
  }
}
