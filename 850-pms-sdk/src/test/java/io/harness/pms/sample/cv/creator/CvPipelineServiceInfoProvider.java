package io.harness.pms.sample.cv.creator;

import io.harness.pms.plan.creator.filters.FilterJsonCreator;
import io.harness.pms.plan.creator.filters.PipelineFilterJsonCreator;
import io.harness.pms.plan.creator.plan.PartialPlanCreator;
import io.harness.pms.plan.creator.plan.PipelinePlanCreator;
import io.harness.pms.plan.creator.plan.PipelineServiceInfoProvider;
import io.harness.pms.steps.StepInfo;
import io.harness.pms.steps.StepMetaData;

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

  @Override
  public List<StepInfo> getStepInfo() {
    List<StepInfo> stepInfos = new ArrayList<>();
    stepInfos.add(StepInfo.newBuilder()
                      .setName("appdVerify")
                      .setStepMetaData(StepMetaData.newBuilder().setFolderPath("Verification").build())
                      .build());
    return stepInfos;
  }
}
