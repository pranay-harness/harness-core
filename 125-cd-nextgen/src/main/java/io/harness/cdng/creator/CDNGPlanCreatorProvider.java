package io.harness.cdng.creator;

import io.harness.cdng.creator.filters.DeploymentStageFilterJsonCreator;
import io.harness.cdng.creator.plan.rollback.RollbackPlanCreator;
import io.harness.cdng.creator.plan.stage.DeploymentStagePMSPlanCreator;
import io.harness.cdng.creator.plan.steps.CDPMSStepPlanCreator;
import io.harness.cdng.creator.variables.DeploymentStageVariableCreator;
import io.harness.cdng.creator.variables.K8sStepVariableCreator;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.execution.ExecutionPMSPlanCreator;
import io.harness.plancreator.steps.StepGroupPMSPlanCreator;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.contracts.steps.StepMetaData;
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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Singleton
public class CDNGPlanCreatorProvider implements PipelineServiceInfoProvider {
  @Inject InjectorUtils injectorUtils;
  @Override
  public List<PartialPlanCreator<?>> getPlanCreators() {
    List<PartialPlanCreator<?>> planCreators = new LinkedList<>();
    planCreators.add(new DeploymentStagePMSPlanCreator());
    planCreators.add(new ExecutionPMSPlanCreator());
    planCreators.add(new StepGroupPMSPlanCreator());
    planCreators.add(new CDPMSStepPlanCreator());
    planCreators.add(new RollbackPlanCreator());
    injectorUtils.injectMembers(planCreators);
    return planCreators;
  }

  @Override
  public List<FilterJsonCreator> getFilterJsonCreators() {
    List<FilterJsonCreator> filterJsonCreators = new ArrayList<>();
    filterJsonCreators.add(new PipelineFilterJsonCreator());
    filterJsonCreators.add(new ParallelFilterJsonCreator());
    filterJsonCreators.add(new DeploymentStageFilterJsonCreator());
    injectorUtils.injectMembers(filterJsonCreators);

    return filterJsonCreators;
  }

  @Override
  public List<VariableCreator> getVariableCreators() {
    List<VariableCreator> variableCreators = new ArrayList<>();
    variableCreators.add(new PipelineVariableCreator());
    variableCreators.add(new DeploymentStageVariableCreator());
    variableCreators.add(new ExecutionVariableCreator());
    variableCreators.add(new StepGroupVariableCreator());
    variableCreators.add(new K8sStepVariableCreator());
    return variableCreators;
  }

  @Override
  public List<StepInfo> getStepInfo() {
    StepInfo apply =
        StepInfo.newBuilder()
            .setName("Apply")
            .setType(StepSpecTypeConstants.PLACEHOLDER)
            .setStepMetaData(StepMetaData.newBuilder().addCategory("Kubernetes").setFolderPath("Kubernetes").build())
            .build();
    StepInfo scale =
        StepInfo.newBuilder()
            .setName("Delete")
            .setType(StepSpecTypeConstants.PLACEHOLDER)
            .setStepMetaData(StepMetaData.newBuilder().addCategory("Kubernetes").setFolderPath("Kubernetes").build())
            .build();
    StepInfo stageDeployment =
        StepInfo.newBuilder()
            .setName("Stage Deployment")
            .setType(StepSpecTypeConstants.PLACEHOLDER)
            .setStepMetaData(StepMetaData.newBuilder().addCategory("Kubernetes").setFolderPath("Kubernetes").build())
            .build();
    StepInfo k8sRolling =
        StepInfo.newBuilder()
            .setName("K8s Rolling")
            .setType(StepSpecTypeConstants.K8S_ROLLING_DEPLOY)
            .setStepMetaData(StepMetaData.newBuilder().addCategory("Kubernetes").setFolderPath("Kubernetes").build())
            .build();
    StepInfo k8sRollingRollback =
        StepInfo.newBuilder()
            .setName("K8s Rolling Rollback")
            .setType(StepSpecTypeConstants.K8S_ROLLING_DEPLOY)
            .setStepMetaData(StepMetaData.newBuilder().addCategory("Kubernetes").setFolderPath("Kubernetes").build())
            .build();
    List<StepInfo> stepInfos = new ArrayList<>();
    stepInfos.add(apply);
    stepInfos.add(scale);
    stepInfos.add(stageDeployment);
    stepInfos.add(k8sRolling);
    stepInfos.add(k8sRollingRollback);
    return stepInfos;
  }
}
