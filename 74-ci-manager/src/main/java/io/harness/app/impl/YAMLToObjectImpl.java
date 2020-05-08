package io.harness.app.impl;

import static java.util.Arrays.asList;

import graph.StepGraph;
import io.harness.app.intfc.YAMLToObject;
import io.harness.beans.CIPipeline;
import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.environment.pod.PodSetupInfo;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.script.ScriptInfo;
import io.harness.beans.stages.JobStage;
import io.harness.beans.stages.StageInfo;
import io.harness.beans.steps.BuildEnvSetupStepInfo;
import io.harness.beans.steps.BuildStepInfo;
import io.harness.beans.steps.CIStep;
import io.harness.beans.steps.StepMetadata;
import io.harness.beans.steps.TestStepInfo;

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

  private List<ScriptInfo> getBuildCommandSteps() {
    return asList(ScriptInfo.builder().scriptString(BUILD_SCRIPT).build());
  }

  private List<StageInfo> getStages() {
    return asList(JobStage.builder()
                      .stepInfos(StepGraph.builder()
                                     .ciSteps(asList(CIStep.builder()
                                                         .stepInfo(BuildEnvSetupStepInfo.builder()
                                                                       .name(ENV_SETUP_NAME)
                                                                       .buildJobEnvInfo(getCIBuildJobEnvInfo())
                                                                       .build())
                                                         .stepMetadata(StepMetadata.builder().build())
                                                         .build(),
                                         CIStep.builder()
                                             .stepInfo(TestStepInfo.builder()
                                                           .name(BUILD_STEP_NAME)
                                                           .scriptInfos(getBuildCommandSteps())
                                                           .build())
                                             .stepMetadata(StepMetadata.builder().build())
                                             .build(),

                                         CIStep.builder()
                                             .stepInfo(BuildStepInfo.builder()
                                                           .name(TEST_STEP_NAME)
                                                           .scriptInfos(getBuildCommandSteps())
                                                           .build())
                                             .stepMetadata(StepMetadata.builder().build())
                                             .build()))
                                     .build())
                      .build());
  }

  private BuildJobEnvInfo getCIBuildJobEnvInfo() {
    return K8BuildJobEnvInfo.builder().podsSetupInfo(getCIPodsSetupInfo()).build();
  }

  private K8BuildJobEnvInfo.PodsSetupInfo getCIPodsSetupInfo() {
    List<PodSetupInfo> pods = new ArrayList<>();
    pods.add(PodSetupInfo.builder()
                 .podSetupParams(PodSetupInfo.PodSetupParams.builder()
                                     .containerDefinitionInfos(Arrays.asList(ContainerDefinitionInfo.builder().build()))
                                     .build())
                 .build());
    return K8BuildJobEnvInfo.PodsSetupInfo.builder().pods(pods).build();
  }
}
