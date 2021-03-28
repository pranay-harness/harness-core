package io.harness.pms.plan.creation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.approval.ApprovalStageFilterJsonCreator;
import io.harness.plancreator.approval.ApprovalStagePlanCreator;
import io.harness.plancreator.execution.ExecutionPMSPlanCreator;
import io.harness.plancreator.pipeline.NGPipelinePlanCreator;
import io.harness.plancreator.stages.StagesPlanCreator;
import io.harness.plancreator.stages.parallel.ParallelPlanCreator;
import io.harness.plancreator.steps.StepGroupPMSPlanCreator;
import io.harness.plancreator.steps.internal.PMSStepPlanCreator;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.sdk.core.pipeline.filters.FilterJsonCreator;
import io.harness.pms.sdk.core.pipeline.filters.ParallelFilterJsonCreator;
import io.harness.pms.sdk.core.pipeline.filters.PipelineFilterJsonCreator;
import io.harness.pms.sdk.core.pipeline.variables.ExecutionVariableCreator;
import io.harness.pms.sdk.core.pipeline.variables.PipelineVariableCreator;
import io.harness.pms.sdk.core.pipeline.variables.StepGroupVariableCreator;
import io.harness.pms.sdk.core.plan.creation.creators.PartialPlanCreator;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;
import io.harness.pms.sdk.core.variables.VariableCreator;
import io.harness.pms.utils.InjectorUtils;
import io.harness.pms.variables.HTTPStepVariableCreator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class PipelineServiceInternalInfoProvider implements PipelineServiceInfoProvider {
  @Inject InjectorUtils injectorUtils;

  @Override
  public List<PartialPlanCreator<?>> getPlanCreators() {
    List<PartialPlanCreator<?>> planCreators = new ArrayList<>();
    planCreators.add(new NGPipelinePlanCreator());
    planCreators.add(new StagesPlanCreator());
    planCreators.add(new ParallelPlanCreator());
    planCreators.add(new PMSStepPlanCreator());
    planCreators.add(new ApprovalStagePlanCreator());
    planCreators.add(new StepGroupPMSPlanCreator());
    planCreators.add(new ExecutionPMSPlanCreator());
    injectorUtils.injectMembers(planCreators);
    return planCreators;
  }

  @Override
  public List<FilterJsonCreator> getFilterJsonCreators() {
    List<FilterJsonCreator> filterJsonCreators = new ArrayList<>();
    filterJsonCreators.add(new PipelineFilterJsonCreator());
    filterJsonCreators.add(new ParallelFilterJsonCreator());
    filterJsonCreators.add(new ApprovalStageFilterJsonCreator());
    injectorUtils.injectMembers(filterJsonCreators);
    return filterJsonCreators;
  }

  @Override
  public List<VariableCreator> getVariableCreators() {
    List<VariableCreator> variableCreators = new ArrayList<>();
    variableCreators.add(new PipelineVariableCreator());
    variableCreators.add(new HTTPStepVariableCreator());
    variableCreators.add(new StepGroupVariableCreator());
    variableCreators.add(new ExecutionVariableCreator());
    injectorUtils.injectMembers(variableCreators);
    return variableCreators;
  }

  @Override
  public List<StepInfo> getStepInfo() {
    return Collections.emptyList();
  }
}
