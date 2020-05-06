package io.harness.app.impl;

import static java.util.Arrays.asList;

import graph.CIStepsGraph;
import io.harness.app.intfc.YAMLToObject;
import io.harness.beans.CIPipeline;
import io.harness.beans.environment.CIBuildJobEnvInfo;
import io.harness.beans.environment.CIK8BuildJobEnvInfo;
import io.harness.beans.environment.pod.CIPodSetupInfo;
import io.harness.beans.environment.pod.container.CIContainerDefinitionInfo;
import io.harness.beans.script.CIScriptInfo;
import io.harness.beans.stages.CIJobStage;
import io.harness.beans.stages.CIStageInfo;
import io.harness.beans.steps.CIBuildEnvSetupStepInfo;
import io.harness.beans.steps.CIBuildStepInfo;
import io.harness.beans.steps.CIStep;
import io.harness.beans.steps.CIStepMetadata;
import io.harness.beans.steps.CITestStepInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Converts YAML to object
 */

public class YAMLToObjectImpl implements YAMLToObject<CIPipeline> {
  private static final String ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";
  private static final String NAME = "CIPipeline";
  private static final String DESCRIPTION = "test pipeline";
  private static final String BUILD_STEP_NAME = "buildStep";
  private static final String ENV_SETUP_NAME = "envSetupName";
  private static final String TEST_STEP_NAME = "testStep";
  private static final String BUILD_SCRIPT = "mvn clean install";

  @Override
  public CIPipeline convertYAML(String yaml) {
    // TODO Add conversion implementation
    return CIPipeline.builder()
        .name(NAME)
        .description(DESCRIPTION)
        .accountId(ACCOUNT_ID)
        .linkedStages(getStages())
        .build();
  }

  private List<CIScriptInfo> getBuildCommandSteps() {
    return asList(CIScriptInfo.builder().scriptString(BUILD_SCRIPT).build());
  }

  private List<CIStageInfo> getStages() {
    return asList(CIJobStage.builder()
                      .stepInfos(CIStepsGraph.builder()
                                     .ciSteps(asList(CIStep.builder()
                                                         .ciStepInfo(CIBuildEnvSetupStepInfo.builder()
                                                                         .name(ENV_SETUP_NAME)
                                                                         .ciBuildJobEnvInfo(getCIBuildJobEnvInfo())
                                                                         .build())
                                                         .ciStepMetadata(CIStepMetadata.builder().build())
                                                         .build(),
                                         CIStep.builder()
                                             .ciStepInfo(CITestStepInfo.builder()
                                                             .name(BUILD_STEP_NAME)
                                                             .scriptInfos(getBuildCommandSteps())
                                                             .build())
                                             .ciStepMetadata(CIStepMetadata.builder().build())
                                             .build(),

                                         CIStep.builder()
                                             .ciStepInfo(CIBuildStepInfo.builder()
                                                             .name(TEST_STEP_NAME)
                                                             .scriptInfos(getBuildCommandSteps())
                                                             .build())
                                             .ciStepMetadata(CIStepMetadata.builder().build())
                                             .build()))
                                     .build())
                      .build());
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
