package io.harness.pms.plan.creation;

import io.harness.OrchestrationStepTypes;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cf.pipeline.CfExecutionPMSPlanCreator;
import io.harness.cf.pipeline.FeatureFlagStageFilterJsonCreator;
import io.harness.cf.pipeline.FeatureFlagStagePlanCreator;
import io.harness.filters.ExecutionPMSFilterJsonCreator;
import io.harness.filters.ParallelFilterJsonCreator;
import io.harness.filters.PipelineFilterJsonCreator;
import io.harness.filters.StepGroupPmsFilterJsonCreator;
import io.harness.plancreator.approval.ApprovalStageFilterJsonCreator;
import io.harness.plancreator.approval.ApprovalStagePlanCreator;
import io.harness.plancreator.execution.ExecutionPmsPlanCreator;
import io.harness.plancreator.pipeline.NGPipelinePlanCreator;
import io.harness.plancreator.stages.StagesPlanCreator;
import io.harness.plancreator.stages.parallel.ParallelPlanCreator;
import io.harness.plancreator.steps.StepGroupPMSPlanCreator;
import io.harness.plancreator.steps.http.HttpStepPlanCreator;
import io.harness.plancreator.steps.internal.PMSStepPlanCreator;
import io.harness.plancreator.steps.internal.PmsStepFilterJsonCreator;
import io.harness.plancreator.steps.internal.PmsStepFilterJsonCreatorV2;
import io.harness.plancreator.steps.resourceconstraint.ResourceConstraintStepPlanCreator;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.contracts.steps.StepMetaData;
import io.harness.pms.sdk.core.pipeline.filters.FilterJsonCreator;
import io.harness.pms.sdk.core.pipeline.variables.ApprovalStageVariableCreator;
import io.harness.pms.sdk.core.pipeline.variables.ExecutionVariableCreator;
import io.harness.pms.sdk.core.pipeline.variables.PipelineVariableCreator;
import io.harness.pms.sdk.core.pipeline.variables.StepGroupVariableCreator;
import io.harness.pms.sdk.core.plan.creation.creators.PartialPlanCreator;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;
import io.harness.pms.sdk.core.variables.VariableCreator;
import io.harness.pms.utils.InjectorUtils;
import io.harness.pms.variables.HTTPStepVariableCreator;
import io.harness.steps.cf.FlagConfigurationStep;
import io.harness.steps.shellscript.ShellScriptStepVariableCreator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
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
    planCreators.add(new HttpStepPlanCreator());
    planCreators.add(new ApprovalStagePlanCreator());
    planCreators.add(new ExecutionPmsPlanCreator());
    planCreators.add(new StepGroupPMSPlanCreator());
    planCreators.add(new ResourceConstraintStepPlanCreator());
    planCreators.add(new FeatureFlagStagePlanCreator());
    planCreators.add(new CfExecutionPMSPlanCreator());
    injectorUtils.injectMembers(planCreators);
    return planCreators;
  }

  @Override
  public List<FilterJsonCreator> getFilterJsonCreators() {
    List<FilterJsonCreator> filterJsonCreators = new ArrayList<>();
    filterJsonCreators.add(new PipelineFilterJsonCreator());
    filterJsonCreators.add(new ParallelFilterJsonCreator());
    filterJsonCreators.add(new ApprovalStageFilterJsonCreator());
    filterJsonCreators.add(new PmsStepFilterJsonCreator());
    filterJsonCreators.add(new PmsStepFilterJsonCreatorV2());
    filterJsonCreators.add(new ExecutionPMSFilterJsonCreator());
    filterJsonCreators.add(new StepGroupPmsFilterJsonCreator());
    filterJsonCreators.add(new FeatureFlagStageFilterJsonCreator());
    injectorUtils.injectMembers(filterJsonCreators);
    return filterJsonCreators;
  }

  @Override
  public List<VariableCreator> getVariableCreators() {
    List<VariableCreator> variableCreators = new ArrayList<>();
    variableCreators.add(new PipelineVariableCreator());
    variableCreators.add(new HTTPStepVariableCreator());
    variableCreators.add(new StepGroupVariableCreator());
    variableCreators.add(new ShellScriptStepVariableCreator());
    variableCreators.add(new ExecutionVariableCreator());
    variableCreators.add(new ApprovalStageVariableCreator());
    injectorUtils.injectMembers(variableCreators);
    return variableCreators;
  }

  @Override
  public List<StepInfo> getStepInfo() {
    StepInfo k8sRolling = StepInfo.newBuilder()
                              .setName(FlagConfigurationStep.STEP_NAME)
                              .setType(OrchestrationStepTypes.FLAG_CONFIGURATION)
                              .setStepMetaData(StepMetaData.newBuilder()
                                                   .addCategory(FlagConfigurationStep.STEP_CATEGORY)
                                                   .addFolderPaths("Feature Flags")
                                                   .build())
                              .build();

    List<StepInfo> stepInfos = new ArrayList<>();

    stepInfos.add(k8sRolling);
    return stepInfos;
  }
}
