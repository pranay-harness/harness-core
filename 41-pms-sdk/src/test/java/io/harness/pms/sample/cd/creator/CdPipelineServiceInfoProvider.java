package io.harness.pms.sample.cd.creator;

import io.harness.pms.sample.cd.creator.filters.DeploymentStageFilterCreator;
import io.harness.pms.sdk.creator.PartialPlanCreator;
import io.harness.pms.sdk.creator.PipelinePlanCreator;
import io.harness.pms.sdk.creator.PipelineServiceInfoProvider;
import io.harness.pms.sdk.creator.filters.FilterJsonCreator;
import io.harness.pms.sdk.creator.filters.PipelineFilterJsonCreator;

import java.util.ArrayList;
import java.util.List;

public class CdPipelineServiceInfoProvider implements PipelineServiceInfoProvider {
  @Override
  public String getServiceName() {
    return "cd";
  }

  @Override
  public List<PartialPlanCreator<?>> getPlanCreators() {
    List<PartialPlanCreator<?>> planCreators = new ArrayList<>();
    planCreators.add(new PipelinePlanCreator());
    planCreators.add(new DeploymentStagePlanCreator());
    planCreators.add(new CdStepPlanCreator());
    return planCreators;
  }

  @Override
  public List<FilterJsonCreator> getFilterJsonCreators() {
    List<FilterJsonCreator> filterJsonCreators = new ArrayList<>();
    filterJsonCreators.add(new PipelineFilterJsonCreator());
    filterJsonCreators.add(new DeploymentStageFilterCreator());

    return filterJsonCreators;
  }
}
