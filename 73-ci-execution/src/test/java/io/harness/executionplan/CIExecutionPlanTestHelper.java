package io.harness.executionplan;

import static java.util.Arrays.asList;

import com.google.inject.Singleton;

import graph.CIStepsGraph;
import io.harness.beans.environment.CIBuildJobEnvInfo;
import io.harness.beans.environment.CIK8BuildJobEnvInfo;
import io.harness.beans.environment.pod.CIPodSetupInfo;
import io.harness.beans.environment.pod.container.CIContainerDefinitionInfo;
import io.harness.beans.script.CIScriptInfo;
import io.harness.beans.steps.CIBuildEnvSetupStepInfo;
import io.harness.beans.steps.CIStep;
import io.harness.beans.steps.CIStepMetadata;
import io.harness.beans.steps.CITestStepInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Singleton
public class CIExecutionPlanTestHelper {
  private static final String BUILD_STAGE_NAME = "buildStage";
  private static final String ENV_SETUP_NAME = "envSetupName";
  private static final String BUILD_SCRIPT = "mvn clean install";
  private List<CIScriptInfo> getBuildCommandSteps() {
    return asList(CIScriptInfo.builder().scriptString(BUILD_SCRIPT).build());
  }

  public CIStepsGraph getStepsGraph() {
    return CIStepsGraph.builder()
        .ciSteps(asList(CIStep.builder()
                            .ciStepInfo(CIBuildEnvSetupStepInfo.builder()
                                            .name(ENV_SETUP_NAME)
                                            .ciBuildJobEnvInfo(getCIBuildJobEnvInfo())
                                            .build())
                            .ciStepMetadata(CIStepMetadata.builder().build())
                            .build(),
            CIStep.builder()
                .ciStepInfo(CITestStepInfo.builder().name(BUILD_STAGE_NAME).scriptInfos(getBuildCommandSteps()).build())
                .ciStepMetadata(CIStepMetadata.builder().build())
                .build()))
        .build();
  }

  private CIBuildJobEnvInfo getCIBuildJobEnvInfo() {
    return CIK8BuildJobEnvInfo.builder().ciPodsSetupInfo(getCIPodsSetupInfo()).build();
  }

  private CIK8BuildJobEnvInfo.CIPodsSetupInfo getCIPodsSetupInfo() {
    List<CIPodSetupInfo> pods = new ArrayList<>();
    pods.add(CIPodSetupInfo.builder()
                 .podSetupParams(CIPodSetupInfo.PodSetupParams.builder()
                                     .containerInfos(Arrays.asList(CIContainerDefinitionInfo.builder().build()))
                                     .build())
                 .build());
    return CIK8BuildJobEnvInfo.CIPodsSetupInfo.builder().pods(pods).build();
  }
}
