package io.harness.cdng.creator;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.creator.filters.DeploymentStageFilterJsonCreator;
import io.harness.cdng.creator.plan.stage.DeploymentStagePMSPlanCreator;
import io.harness.cdng.creator.plan.steps.CDPMSStepFilterJsonCreator;
import io.harness.cdng.creator.plan.steps.CDPMSStepPlanCreator;
import io.harness.cdng.creator.variables.DeploymentStageVariableCreator;
import io.harness.cdng.creator.variables.K8sStepVariableCreator;
import io.harness.cdng.creator.variables.ShellScriptStepVariableCreator;
import io.harness.cdng.provision.terraform.variablecreator.TerraformStepsVariableCreator;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.contracts.steps.StepMetaData;
import io.harness.pms.sdk.core.pipeline.filters.FilterJsonCreator;
import io.harness.pms.sdk.core.pipeline.variables.ExecutionVariableCreator;
import io.harness.pms.sdk.core.plan.creation.creators.PartialPlanCreator;
import io.harness.pms.sdk.core.plan.creation.creators.PipelineServiceInfoProvider;
import io.harness.pms.sdk.core.variables.VariableCreator;
import io.harness.pms.utils.InjectorUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@OwnedBy(HarnessTeam.CDC)
@Singleton
public class CDNGPlanCreatorProvider implements PipelineServiceInfoProvider {
  @Inject InjectorUtils injectorUtils;
  @Override
  public List<PartialPlanCreator<?>> getPlanCreators() {
    List<PartialPlanCreator<?>> planCreators = new LinkedList<>();
    planCreators.add(new DeploymentStagePMSPlanCreator());
    planCreators.add(new CDPMSStepPlanCreator());
    injectorUtils.injectMembers(planCreators);
    return planCreators;
  }

  @Override
  public List<FilterJsonCreator> getFilterJsonCreators() {
    List<FilterJsonCreator> filterJsonCreators = new ArrayList<>();
    filterJsonCreators.add(new DeploymentStageFilterJsonCreator());
    filterJsonCreators.add(new CDPMSStepFilterJsonCreator());
    injectorUtils.injectMembers(filterJsonCreators);

    return filterJsonCreators;
  }

  @Override
  public List<VariableCreator> getVariableCreators() {
    List<VariableCreator> variableCreators = new ArrayList<>();
    variableCreators.add(new DeploymentStageVariableCreator());
    variableCreators.add(new ExecutionVariableCreator());
    variableCreators.add(new K8sStepVariableCreator());
    variableCreators.add(new TerraformStepsVariableCreator());
    variableCreators.add(new ShellScriptStepVariableCreator());
    return variableCreators;
  }

  @Override
  public List<StepInfo> getStepInfo() {
    StepInfo k8sRolling =
        StepInfo.newBuilder()
            .setName("Rolling Deployment")
            .setType(StepSpecTypeConstants.K8S_ROLLING_DEPLOY)
            .setStepMetaData(StepMetaData.newBuilder().addCategory("Kubernetes").setFolderPath("Kubernetes").build())
            .build();

    StepInfo canaryDeploy =
        StepInfo.newBuilder()
            .setName("Canary Deployment")
            .setType(StepSpecTypeConstants.K8S_CANARY_DEPLOY)
            .setStepMetaData(StepMetaData.newBuilder().addCategory("Kubernetes").setFolderPath("Kubernetes").build())
            .build();
    StepInfo canaryDelete =
        StepInfo.newBuilder()
            .setName("Canary Delete")
            .setType(StepSpecTypeConstants.K8S_CANARY_DELETE)
            .setStepMetaData(StepMetaData.newBuilder().addCategory("Kubernetes").setFolderPath("Kubernetes").build())
            .build();
    StepInfo delete =
        StepInfo.newBuilder()
            .setName("Delete")
            .setType(StepSpecTypeConstants.K8S_DELETE)
            .setStepMetaData(StepMetaData.newBuilder().addCategory("Kubernetes").setFolderPath("Kubernetes").build())
            .build();

    StepInfo stageDeployment =
        StepInfo.newBuilder()
            .setName("Stage Deployment")
            .setType(StepSpecTypeConstants.K8S_BLUE_GREEN_DEPLOY)
            .setStepMetaData(StepMetaData.newBuilder().addCategory("Kubernetes").setFolderPath("Kubernetes").build())
            .build();
    StepInfo bgSwapServices =
        StepInfo.newBuilder()
            .setName("BG Swap Services")
            .setType(StepSpecTypeConstants.K8S_BG_SWAP_SERVICES)
            .setStepMetaData(StepMetaData.newBuilder().addCategory("Kubernetes").setFolderPath("Kubernetes").build())
            .build();

    StepInfo apply =
        StepInfo.newBuilder()
            .setName("Apply")
            .setType(StepSpecTypeConstants.K8S_APPLY)
            .setStepMetaData(StepMetaData.newBuilder().addCategory("Kubernetes").setFolderPath("Kubernetes").build())
            .build();
    StepInfo scale =
        StepInfo.newBuilder()
            .setName("Scale")
            .setType(StepSpecTypeConstants.K8S_SCALE)
            .setStepMetaData(StepMetaData.newBuilder().addCategory("Kubernetes").setFolderPath("Kubernetes").build())
            .build();

    StepInfo k8sRollingRollback =
        StepInfo.newBuilder()
            .setName("Rolling Rollback")
            .setType(StepSpecTypeConstants.K8S_ROLLING_ROLLBACK)
            .setStepMetaData(StepMetaData.newBuilder().addCategory("Kubernetes").setFolderPath("Kubernetes").build())
            .build();
    List<StepInfo> stepInfos = new ArrayList<>();

    stepInfos.add(k8sRolling);
    stepInfos.add(delete);
    stepInfos.add(canaryDeploy);
    stepInfos.add(canaryDelete);
    stepInfos.add(stageDeployment);
    stepInfos.add(bgSwapServices);
    stepInfos.add(apply);
    stepInfos.add(scale);
    stepInfos.add(k8sRollingRollback);
    return stepInfos;
  }
}
