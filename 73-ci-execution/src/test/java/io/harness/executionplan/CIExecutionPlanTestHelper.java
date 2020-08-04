package io.harness.executionplan;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static software.wings.beans.ci.pod.CIContainerType.STEP_EXECUTOR;

import com.google.inject.Singleton;

import graph.StepInfoGraph;
import io.harness.beans.CIPipeline;
import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.environment.pod.PodSetupInfo;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.environment.pod.container.ContainerImageDetails;
import io.harness.beans.script.ScriptInfo;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.steps.stepinfo.BuildEnvSetupStepInfo;
import io.harness.beans.steps.stepinfo.CleanupStepInfo;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.TestStepInfo;
import io.harness.beans.steps.stepinfo.publish.artifact.DockerFileArtifact;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.EcrConnector;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.GcrConnector;
import io.harness.beans.yaml.extended.CustomVariables;
import io.harness.beans.yaml.extended.connector.GitConnectorYaml;
import io.harness.beans.yaml.extended.container.Container;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.common.CIExecutionConstants;
import io.harness.k8s.model.ImageDetails;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import io.harness.yaml.core.intfc.Connector;
import io.harness.yaml.core.intfc.Infrastructure;
import software.wings.beans.ci.pod.ContainerResourceParams;
import software.wings.beans.ci.pod.EncryptedVariableWithType;
import software.wings.beans.ci.pod.PVCParams;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class CIExecutionPlanTestHelper {
  private static final String BUILD_STAGE_NAME = "buildStage";
  private static final String ENV_SETUP_NAME = "envSetupName";
  private static final String BUILD_SCRIPT = "mvn clean install";

  private final ImageDetails imageDetails = ImageDetails.builder().name("maven").tag("3.6.3-jdk-8").build();

  public List<ScriptInfo> getBuildCommandSteps() {
    return singletonList(ScriptInfo.builder().scriptString(BUILD_SCRIPT).build());
  }

  public List<ExecutionWrapper> getExpectedExecutionWrappers() {
    return Arrays.asList(StepElement.builder()
                             .identifier("liteEngineTask1")
                             .stepSpecType(LiteEngineTaskStepInfo.builder()
                                               .branchName("master")
                                               .identifier("liteEngineTask1")
                                               .buildJobEnvInfo(getCIBuildJobEnvInfoOnFirstPod())
                                               .gitConnectorIdentifier("testGitConnector")
                                               .usePVC(true)
                                               .steps(getExpectedExecutionElement())
                                               .build())
                             .build(),
        StepElement.builder()
            .identifier("cleanup")
            .type("cleanup")
            .stepSpecType(CleanupStepInfo.builder().identifier("cleanup").build())
            .build());
  }

  public LiteEngineTaskStepInfo getExpectedLiteEngineTaskInfoOnFirstPod() {
    return LiteEngineTaskStepInfo.builder()
        .branchName("master")
        .identifier("liteEngineTask1")
        .buildJobEnvInfo(getCIBuildJobEnvInfoOnFirstPod())
        .gitConnectorIdentifier("testGitConnector")
        .usePVC(true)
        .steps(getExecutionElement())
        .build();
  }

  public LiteEngineTaskStepInfo getExpectedLiteEngineTaskInfoOnOtherPods() {
    return LiteEngineTaskStepInfo.builder()
        .identifier("liteEngineTask2")
        .buildJobEnvInfo(getCIBuildJobEnvInfoOnOtherPods())
        .usePVC(true)
        .steps(getExecutionElement())
        .build();
  }

  public BuildEnvSetupStepInfo getBuildEnvSetupStepInfoOnFirstPod() {
    return BuildEnvSetupStepInfo.builder()
        .identifier(ENV_SETUP_NAME)
        .buildJobEnvInfo(getCIBuildJobEnvInfoOnFirstPod())
        .branchName("master")
        .gitConnectorIdentifier("testGitConnector")
        .build();
  }
  public BuildEnvSetupStepInfo getBuildEnvSetupStepInfoOnOtherPods() {
    return BuildEnvSetupStepInfo.builder()
        .identifier(ENV_SETUP_NAME)
        .buildJobEnvInfo(getCIBuildJobEnvInfoOnOtherPods())
        .build();
  }

  public BuildJobEnvInfo getCIBuildJobEnvInfoOnFirstPod() {
    return K8BuildJobEnvInfo.builder()
        .workDir("/root")
        .podsSetupInfo(getCIPodsSetupInfoOnFirstPod())
        .publishStepConnectorIdentifier(getPublishArtifactConnectorIds())
        .build();
  }

  public BuildJobEnvInfo getCIBuildJobEnvInfoOnOtherPods() {
    return K8BuildJobEnvInfo.builder()
        .workDir("/root")
        .podsSetupInfo(getCIPodsSetupInfoOnOtherPods())
        .publishStepConnectorIdentifier(getPublishArtifactConnectorIds())
        .build();
  }

  public K8BuildJobEnvInfo.PodsSetupInfo getCIPodsSetupInfoOnFirstPod() {
    List<PodSetupInfo> pods = new ArrayList<>();
    pods.add(PodSetupInfo.builder()
                 .name("")
                 .pvcParams(PVCParams.builder()
                                .volumeName("step-exec")
                                .claimName("")
                                .isPresent(false)
                                .sizeMib(CIExecutionConstants.PVC_DEFAULT_STORAGE_SIZE)
                                .storageClass(CIExecutionConstants.PVC_DEFAULT_STORAGE_CLASS)
                                .build())
                 .podSetupParams(PodSetupInfo.PodSetupParams.builder()
                                     .containerDefinitionInfos(
                                         asList(getMainContainerDefinitionInfo(), getOtherContainerDefinitionInfo()))
                                     .build())
                 .build());
    return K8BuildJobEnvInfo.PodsSetupInfo.builder().podSetupInfoList(pods).build();
  }

  public K8BuildJobEnvInfo.PodsSetupInfo getCIPodsSetupInfoOnOtherPods() {
    List<PodSetupInfo> pods = new ArrayList<>();
    pods.add(PodSetupInfo.builder()
                 .pvcParams(PVCParams.builder()
                                .volumeName("step-exec")
                                .claimName("buildnumber22850")
                                .isPresent(true)
                                .sizeMib(CIExecutionConstants.PVC_DEFAULT_STORAGE_SIZE)
                                .storageClass(CIExecutionConstants.PVC_DEFAULT_STORAGE_CLASS)
                                .build())
                 .podSetupParams(PodSetupInfo.PodSetupParams.builder()
                                     .containerDefinitionInfos(
                                         asList(getMainContainerDefinitionInfo(), getOtherContainerDefinitionInfo()))
                                     .build())
                 .build());
    return K8BuildJobEnvInfo.PodsSetupInfo.builder().podSetupInfoList(pods).build();
  }

  private ContainerDefinitionInfo getOtherContainerDefinitionInfo() {
    return ContainerDefinitionInfo.builder()
        .containerImageDetails(
            ContainerImageDetails.builder().connectorIdentifier("testConnector").imageDetails(imageDetails).build())
        .name("build-setup2")
        .containerType(STEP_EXECUTOR)
        .isMainLiteEngine(false)
        .ports(singletonList(9001))
        .containerResourceParams(ContainerResourceParams.builder()
                                     .resourceRequestMilliCpu(1000)
                                     .resourceRequestMemoryMiB(1000)
                                     .resourceLimitMilliCpu(1000)
                                     .resourceLimitMemoryMiB(1000)
                                     .build())
        .envVars(getEnvVars())
        .encryptedSecrets(getEncryptedSecrets())
        .build();
  }

  private ContainerDefinitionInfo getMainContainerDefinitionInfo() {
    return ContainerDefinitionInfo.builder()
        .containerImageDetails(
            ContainerImageDetails.builder().connectorIdentifier("testConnector").imageDetails(imageDetails).build())
        .name("build-setup1")
        .containerType(STEP_EXECUTOR)
        .isMainLiteEngine(true)
        .containerResourceParams(ContainerResourceParams.builder()
                                     .resourceRequestMilliCpu(1000)
                                     .resourceRequestMemoryMiB(1000)
                                     .resourceLimitMilliCpu(1000)
                                     .resourceLimitMemoryMiB(1000)
                                     .build())
        .envVars(getEnvVars())
        .encryptedSecrets(getEncryptedSecrets())
        .build();
  }

  public StepInfoGraph getStepsGraph() {
    return StepInfoGraph.builder()
        .steps(asList(getBuildEnvSetupStepInfoOnFirstPod(),
            TestStepInfo.builder().identifier(BUILD_STAGE_NAME).scriptInfos(getBuildCommandSteps()).build()))
        .build();
  }

  public ExecutionElement getExecutionElement() {
    List<ExecutionWrapper> executionSectionList = getExecutionSectionsWithLESteps();
    executionSectionList.addAll(getCleanupStep());
    return ExecutionElement.builder().steps(executionSectionList).build();
  }

  public ExecutionElement getExpectedExecutionElement() {
    List<ExecutionWrapper> executionSectionList = getExecutionSectionsWithLESteps();
    return ExecutionElement.builder().steps(executionSectionList).build();
  }

  public List<ExecutionWrapper> getExecutionSectionsWithLESteps() {
    return new ArrayList<>(Arrays.asList(
        StepElement.builder()
            .identifier("git-1")
            .type("gitClone")
            .stepSpecType(GitCloneStepInfo.builder().branch("master").identifier("git-1").build())
            .build(),
        StepElement.builder()
            .identifier("run-1")
            .type("run")
            .name("buildScript")
            .stepSpecType(RunStepInfo.builder()
                              .identifier("run-1")
                              .name("buildScript")
                              .command(singletonList("./build-script.sh"))
                              .build())
            .build(),
        ParallelStepElement.builder()
            .sections(asList(StepElement.builder()
                                 .identifier("test-p1")
                                 .type("run")
                                 .name("testScript1")
                                 .stepSpecType(RunStepInfo.builder()
                                                   .identifier("test-p1")
                                                   .name("testScript1")
                                                   .command(singletonList("./test-script1.sh"))
                                                   .build())
                                 .build(),
                StepElement.builder()
                    .identifier("test-p2")
                    .type("run")
                    .name("testScript2")
                    .stepSpecType(RunStepInfo.builder()
                                      .identifier("test-p2")
                                      .name("testScript2")
                                      .command(singletonList("./test-script2.sh"))
                                      .build())
                    .build()))
            .build(),
        ParallelStepElement.builder()
            .sections(asList(StepElement.builder()
                                 .identifier("publish-1")
                                 .type("publishArtifacts")
                                 .stepSpecType(PublishStepInfo.builder()
                                                   .identifier("publish-1")
                                                   .publishArtifacts(singletonList(
                                                       DockerFileArtifact.builder()
                                                           .connector(GcrConnector.builder()
                                                                          .connector("gcr-connector")
                                                                          .location("us.gcr.io/ci-play/portal:v01")
                                                                          .build())
                                                           .tag("v01")
                                                           .image("ci-play/portal")
                                                           .context("~/")
                                                           .dockerFile("~/Dockerfile")
                                                           .build()))
                                                   .build())
                                 .build(),
                StepElement.builder()
                    .identifier("publish-2")
                    .type("publishArtifacts")
                    .stepSpecType(
                        PublishStepInfo.builder()
                            .identifier("publish-2")
                            .publishArtifacts(singletonList(
                                DockerFileArtifact.builder()
                                    .connector(
                                        EcrConnector.builder()
                                            .connector("ecr-connector")
                                            .location(
                                                " https://987923132879.dkr.ecr.eu-west-1.amazonaws.com/ci-play/portal:v01")
                                            .build())
                                    .tag("v01")
                                    .image("ci-play/portal")
                                    .context("~/")
                                    .dockerFile("~/Dockerfile")
                                    .build()))
                            .build())
                    .build()))
            .build()));
  }

  public List<ExecutionWrapper> getCleanupStep() {
    return singletonList(StepElement.builder()
                             .identifier("cleanup")
                             .type("cleanup")
                             .stepSpecType(CleanupStepInfo.builder().identifier("cleanup").build())
                             .build());
  }

  public Set<String> getPublishArtifactConnectorIds() {
    Set<String> ids = new HashSet<>();
    ids.add("gcr-connector");
    ids.add("ecr-connector");
    return ids;
  }

  public CIPipeline getCIPipeline() {
    return CIPipeline.builder().identifier("testPipelineIdentifier").stages(getStages()).build();
  }

  private List<StageElement> getStages() {
    return new ArrayList<>(singletonList(getIntegrationStageElement()));
  }

  public Connector getConnector() {
    return GitConnectorYaml.builder()
        .identifier("testGitConnector")
        .type("git")
        .spec(GitConnectorYaml.Spec.builder()
                  .authScheme(GitConnectorYaml.Spec.AuthScheme.builder().sshKey("testKey").type("ssh").build())
                  .repo("testRepo")
                  .build())
        .build();
  }

  public Container getContainer() {
    return Container.builder()
        .connector("testConnector")
        .identifier("testContainer")
        .imagePath("maven:3.6.3-jdk-8")
        .resources(Container.Resources.builder()
                       .limit(Container.Limit.builder().cpu(1000).memory(1000).build())
                       .reserve(Container.Reserve.builder().cpu(1000).memory(1000).build())
                       .build())
        .build();
  }

  public Infrastructure getInfrastructure() {
    return K8sDirectInfraYaml.builder()
        .type("kubernetes-direct")
        .spec(K8sDirectInfraYaml.Spec.builder()
                  .kubernetesCluster("testKubernetesCluster")
                  .namespace("testNamespace")
                  .build())
        .build();
  }
  public StageElement getIntegrationStageElement() {
    return StageElement.builder().identifier("intStageIdentifier").stageType(getIntegrationStage()).build();
  }

  public IntegrationStage getIntegrationStage() {
    return IntegrationStage.builder()
        .identifier("intStageIdentifier")
        .workingDirectory("/root")
        .execution(getExecutionElement())
        .gitConnector(getConnector())
        .container(getContainer())
        .infrastructure(getInfrastructure())
        .customVariables(getCustomVariables())
        .build();
  }

  private List<CustomVariables> getCustomVariables() {
    CustomVariables var1 = CustomVariables.builder().name("VAR1").type("secret").build();
    CustomVariables var2 = CustomVariables.builder().name("VAR2").type("secret").build();
    CustomVariables var3 = CustomVariables.builder().name("VAR3").type("text").value("value3").build();
    CustomVariables var4 = CustomVariables.builder().name("VAR4").type("text").value("value4").build();
    return asList(var1, var2, var3, var4);
  }

  public Map<String, String> getEnvVars() {
    Map<String, String> envVars = new HashMap<>();
    envVars.put("VAR3", "value3");
    envVars.put("VAR4", "value4");
    return envVars;
  }

  public Map<String, EncryptedVariableWithType> getEncryptedSecrets() {
    Map<String, EncryptedVariableWithType> envVars = new HashMap<>();
    envVars.put("VAR1", EncryptedVariableWithType.builder().build());
    envVars.put("VAR2", EncryptedVariableWithType.builder().build());
    return envVars;
  }
}
