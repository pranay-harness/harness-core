package io.harness.cdng.creator;

import io.harness.pms.plan.creator.filters.FilterJsonCreator;
import io.harness.pms.plan.creator.plan.PartialPlanCreator;
import io.harness.pms.plan.creator.plan.PipelineServiceInfoProvider;
import io.harness.pms.steps.StepInfo;
import io.harness.pms.steps.StepMetaData;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CDNGPlanCreatorProvider implements PipelineServiceInfoProvider {
  @Override
  public String getServiceName() {
    return "cd";
  }

  @Override
  public List<PartialPlanCreator<?>> getPlanCreators() {
    return new LinkedList<>();
  }

  @Override
  public List<FilterJsonCreator> getFilterJsonCreators() {
    return new ArrayList<>();
  }

  @Override
  public List<StepInfo> getStepInfo() {
    StepInfo apply =
        StepInfo.newBuilder()
            .setName("Apply")
            .setStepMetaData(StepMetaData.newBuilder().addCategory("Kubernetes").setFolderPath("Kubernetes").build())
            .build();
    StepInfo scale =
        StepInfo.newBuilder()
            .setName("Delete")
            .setStepMetaData(StepMetaData.newBuilder().addCategory("Kubernetes").setFolderPath("Kubernetes").build())
            .build();
    StepInfo stageDeployment =
        StepInfo.newBuilder()
            .setName("Stage Deployment")
            .setStepMetaData(StepMetaData.newBuilder().addCategory("Kubernetes").setFolderPath("Kubernetes").build())
            .build();
    StepInfo k8sRolling =
        StepInfo.newBuilder()
            .setName("K8s Rolling")
            .setStepMetaData(StepMetaData.newBuilder().addCategory("Kubernetes").setFolderPath("Kubernetes").build())
            .build();
    StepInfo k8sRollingRollback =
        StepInfo.newBuilder()
            .setName("K8s Rolling Rollback")
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
