package io.harness.pms.sample.cv.creator;

import io.harness.pms.sdk.creator.PartialPlanCreator;
import io.harness.pms.sdk.creator.PipelinePlanCreator;
import io.harness.pms.sdk.creator.PipelineServiceInfoProvider;
import io.harness.pms.sdk.creator.filters.FilterJsonCreator;
import io.harness.pms.sdk.creator.filters.PipelineFilterJsonCreator;

import java.util.ArrayList;
import java.util.List;

public class CvPipelineServiceInfoProvider implements PipelineServiceInfoProvider {
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

  @Override
  public List<FilterJsonCreator> getFilterJsonCreators() {
    List<FilterJsonCreator> filterJsonCreators = new ArrayList<>();
    filterJsonCreators.add(new PipelineFilterJsonCreator());
    return filterJsonCreators;
  }
}
