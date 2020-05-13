package io.harness.app.impl;

import static java.util.Arrays.asList;
import static software.wings.common.CICommonPodConstants.POD_NAME;

import graph.StepGraph;
import io.harness.app.intfc.YAMLToObject;
import io.harness.beans.CIPipeline;
import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.environment.pod.PodSetupInfo;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.environment.pod.container.ContainerImageDetails;
import io.harness.beans.environment.pod.container.ContainerType;
import io.harness.beans.script.ScriptInfo;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.stages.StageInfo;
import io.harness.beans.steps.BuildEnvSetupStepInfo;
import io.harness.beans.steps.BuildStepInfo;
import io.harness.beans.steps.CIStep;
import io.harness.beans.steps.CleanupStepInfo;
import io.harness.beans.steps.StepMetadata;
import software.wings.beans.ci.pod.ContainerResourceParams;
import software.wings.beans.container.ImageDetails;

import java.util.ArrayList;
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
  private static final String WORK_DIR = "workspace";
  private static final String CLEANUP_STEP_NAME = "cleanupStep";
  private static final String CONTAINER_NAME = "build-setup";
  private static final String K8_CLUSTER_IDENTIFIER = "kubernetes_clusterqqq";
  private static final String GIT_REPO_IDENTIFIER = "gitRepo";
  private static final String IMAGE_REPO_IDENTIFIER = "not registered";
  private static final ImageDetails imageDetails = ImageDetails.builder()
                                                       .name("maven")
                                                       .tag("3.6.3-jdk-8")
                                                       .registryUrl("https://index.docker.io/v1/")
                                                       .username("harshjain12")
                                                       .build();

  ContainerResourceParams containerResourceParams = ContainerResourceParams.builder()
                                                        .resourceRequestMilliCpu(12000)
                                                        .resourceRequestMemoryMiB(12000)
                                                        .resourceLimitMilliCpu(15000)
                                                        .resourceLimitMemoryMiB(15000)
                                                        .build();

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
    return asList(ScriptInfo.builder().scriptString("cd /step-exec/workspace").build(),
        ScriptInfo.builder().scriptString("mvn clean install -DskipTests --fail-at-end").build());
  }

  private List<StageInfo> getStages() {
    return asList(IntegrationStage.builder()
                      .k8ConnectorIdentifier(K8_CLUSTER_IDENTIFIER)
                      .stepInfos(StepGraph.builder()
                                     .ciSteps(asList(
                                         CIStep.builder()
                                             .stepInfo(BuildEnvSetupStepInfo.builder()
                                                           .gitConnectorIdentifier(GIT_REPO_IDENTIFIER)
                                                           .identifier(ENV_SETUP_NAME)
                                                           .buildJobEnvInfo(getCIBuildJobEnvInfo(K8_CLUSTER_IDENTIFIER))
                                                           .build())
                                             .stepMetadata(StepMetadata.builder().build())
                                             .build(),

                                         CIStep.builder()
                                             .stepInfo(BuildStepInfo.builder()
                                                           .identifier(BUILD_STEP_NAME)
                                                           .scriptInfos(getBuildCommandSteps())
                                                           .build())
                                             .stepMetadata(StepMetadata.builder().build())
                                             .build(),

                                         CIStep.builder()
                                             .stepInfo(CleanupStepInfo.builder().identifier(CLEANUP_STEP_NAME).build())
                                             .stepMetadata(StepMetadata.builder().build())
                                             .build()))
                                     .build())
                      .build());
  }

  private BuildJobEnvInfo getCIBuildJobEnvInfo(String k8Cluster) {
    return K8BuildJobEnvInfo.builder().podsSetupInfo(getCIPodsSetupInfo()).workDir(WORK_DIR).build();
  }

  private K8BuildJobEnvInfo.PodsSetupInfo getCIPodsSetupInfo() {
    List<PodSetupInfo> pods = new ArrayList<>();
    pods.add(PodSetupInfo.builder()
                 .podSetupParams(PodSetupInfo.PodSetupParams.builder()
                                     .containerDefinitionInfos(asList(
                                         ContainerDefinitionInfo.builder()
                                             .containerResourceParams(containerResourceParams)
                                             .containerImageDetails(ContainerImageDetails.builder()
                                                                        .imageDetails(imageDetails)
                                                                        .connectorIdentifier(IMAGE_REPO_IDENTIFIER)
                                                                        .build())
                                             .containerType(ContainerType.STEP_EXECUTOR)
                                             .name(CONTAINER_NAME)
                                             .build()))
                                     .build())
                 .name(POD_NAME)
                 .build());
    return K8BuildJobEnvInfo.PodsSetupInfo.builder().podSetupInfoList(pods).build();
  }
}
