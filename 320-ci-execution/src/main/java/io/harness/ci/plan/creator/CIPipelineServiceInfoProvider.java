package io.harness.ci.plan.creator;

import io.harness.ci.plan.creator.execution.CIExecutionPmsPlanCreator;
import io.harness.ci.plan.creator.filter.CIStageFilterJsonCreator;
import io.harness.ci.plan.creator.stage.IntegrationStagePMSPlanCreator;
import io.harness.ci.plan.creator.step.CIPMSStepPlanCreator;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.sdk.core.pipeline.filters.FilterJsonCreator;
import io.harness.pms.sdk.core.plan.creation.creators.PartialPlanCreator;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;
import io.harness.pms.sdk.core.variables.VariableCreator;
import io.harness.pms.utils.InjectorUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Singleton
public class CIPipelineServiceInfoProvider implements PipelineServiceInfoProvider {
  @Inject InjectorUtils injectorUtils;

  @Override
  public List<PartialPlanCreator<?>> getPlanCreators() {
    List<PartialPlanCreator<?>> planCreators = new LinkedList<>();
    planCreators.add(new IntegrationStagePMSPlanCreator());
    planCreators.add(new CIExecutionPmsPlanCreator());
    planCreators.add(new CIPMSStepPlanCreator());
    injectorUtils.injectMembers(planCreators);
    return planCreators;
  }

  @Override
  public List<FilterJsonCreator> getFilterJsonCreators() {
    List<FilterJsonCreator> filterJsonCreators = new ArrayList<>();
    filterJsonCreators.add(new CIStageFilterJsonCreator());
    injectorUtils.injectMembers(filterJsonCreators);

    return filterJsonCreators;
  }

  @Override
  public List<VariableCreator> getVariableCreators() {
    return null;
  }

  @Override
  public List<StepInfo> getStepInfo() {
    return new ArrayList<>();
  }
}
