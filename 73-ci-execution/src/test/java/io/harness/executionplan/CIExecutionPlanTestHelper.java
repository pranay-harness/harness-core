package io.harness.executionplan;

import static java.util.Arrays.asList;

import com.google.inject.Singleton;

import graph.StepGraph;
import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.environment.pod.PodSetupInfo;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.script.ScriptInfo;
import io.harness.beans.steps.BuildEnvSetupStepInfo;
import io.harness.beans.steps.CIStep;
import io.harness.beans.steps.StepMetadata;
import io.harness.beans.steps.TestStepInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Singleton
public class CIExecutionPlanTestHelper {
  private static final String BUILD_STAGE_NAME = "buildStage";
  private static final String ENV_SETUP_NAME = "envSetupName";
  private static final String BUILD_SCRIPT = "mvn clean install";
  private static final String POD_NAME = "Pod1";

  public List<ScriptInfo> getBuildCommandSteps() {
    return asList(ScriptInfo.builder().scriptString(BUILD_SCRIPT).build());
  }

  public K8BuildJobEnvInfo.PodsSetupInfo getCIPodsSetupInfo() {
    List<PodSetupInfo> pods = new ArrayList<>();
    pods.add(PodSetupInfo.builder()
                 .name(POD_NAME)
                 .podSetupParams(PodSetupInfo.PodSetupParams.builder()
                                     .containerDefinitionInfos(Arrays.asList(ContainerDefinitionInfo.builder().build()))
                                     .build())
                 .build());
    return K8BuildJobEnvInfo.PodsSetupInfo.builder().podSetupInfoList(pods).build();
  }

  public BuildEnvSetupStepInfo getBuildEnvSetupStepInfo() {
    return BuildEnvSetupStepInfo.builder().identifier(ENV_SETUP_NAME).buildJobEnvInfo(getCIBuildJobEnvInfo()).build();
  }

  public StepGraph getStepsGraph() {
    return StepGraph.builder()
        .ciSteps(asList(
            CIStep.builder().stepInfo(getBuildEnvSetupStepInfo()).stepMetadata(StepMetadata.builder().build()).build(),
            CIStep.builder()
                .stepInfo(
                    TestStepInfo.builder().identifier(BUILD_STAGE_NAME).scriptInfos(getBuildCommandSteps()).build())
                .stepMetadata(StepMetadata.builder().build())
                .build()))
        .build();
  }

  private BuildJobEnvInfo getCIBuildJobEnvInfo() {
    return K8BuildJobEnvInfo.builder().podsSetupInfo(getCIPodsSetupInfo()).build();
  }
}
