package io.harness.executionplan;

import static java.util.Arrays.asList;
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
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.steps.stepinfo.TestStepInfo;
import io.harness.beans.steps.stepinfo.publish.artifact.DockerFileArtifact;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.GcrConnector;
import io.harness.beans.yaml.extended.CustomVariables;
import io.harness.beans.yaml.extended.artifact.DockerHubArtifactStreamYaml;
import io.harness.beans.yaml.extended.connector.GitConnectorYaml;
import io.harness.beans.yaml.extended.container.Container;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.yaml.core.Artifact;
import io.harness.yaml.core.Execution;
import io.harness.yaml.core.auxiliary.intfc.ExecutionSection;
import io.harness.yaml.core.auxiliary.intfc.StageWrapper;
import io.harness.yaml.core.intfc.Connector;
import io.harness.yaml.core.intfc.Infrastructure;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.ci.pod.ContainerResourceParams;
import software.wings.beans.container.ImageDetails;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
  private static final String POD_NAME = "build-setup";

  public String getPodName() {
    return POD_NAME;
  }
  private ImageDetails imageDetails = ImageDetails.builder().name("maven").tag("3.6.3-jdk-8").build();

  public List<ScriptInfo> getBuildCommandSteps() {
    return asList(ScriptInfo.builder().scriptString(BUILD_SCRIPT).build());
  }

  public K8BuildJobEnvInfo.PodsSetupInfo getCIPodsSetupInfo() {
    List<PodSetupInfo> pods = new ArrayList<>();
    pods.add(PodSetupInfo.builder()
                 .name(POD_NAME)
                 .podSetupParams(PodSetupInfo.PodSetupParams.builder()
                                     .containerDefinitionInfos(
                                         asList(ContainerDefinitionInfo.builder()
                                                    .containerImageDetails(ContainerImageDetails.builder()
                                                                               .connectorIdentifier("testConnector")
                                                                               .imageDetails(imageDetails)
                                                                               .build())
                                                    .name("build-setup")
                                                    .containerType(STEP_EXECUTOR)
                                                    .containerResourceParams(ContainerResourceParams.builder()
                                                                                 .resourceRequestMilliCpu(1000)
                                                                                 .resourceRequestMemoryMiB(1000)
                                                                                 .resourceLimitMilliCpu(1000)
                                                                                 .resourceLimitMemoryMiB(1000)
                                                                                 .build())
                                                    .envVars(getEnvVars())
                                                    .encryptedSecrets(getEncryptedSecrets())
                                                    .build()))

                                     .build())
                 .build());
    return K8BuildJobEnvInfo.PodsSetupInfo.builder().podSetupInfoList(pods).build();
  }

  public BuildEnvSetupStepInfo getBuildEnvSetupStepInfo() {
    return BuildEnvSetupStepInfo.builder()
        .identifier(ENV_SETUP_NAME)
        .setupEnv(BuildEnvSetupStepInfo.BuildEnvSetup.builder().buildJobEnvInfo(getCIBuildJobEnvInfo()).build())
        .build();
  }

  public StepInfoGraph getStepsGraph() {
    return StepInfoGraph.builder()
        .steps(asList(getBuildEnvSetupStepInfo(),
            TestStepInfo.builder()
                .identifier(BUILD_STAGE_NAME)
                .test(TestStepInfo.Test.builder().scriptInfos(getBuildCommandSteps()).build())
                .build()))
        .build();
  }

  private BuildJobEnvInfo getCIBuildJobEnvInfo() {
    return K8BuildJobEnvInfo.builder().podsSetupInfo(getCIPodsSetupInfo()).build();
  }

  public Execution getExecution() {
    List<ExecutionSection> executionSectionList = getExecutionSections();
    return Execution.builder().steps(executionSectionList).build();
  }

  @NotNull
  public List<ExecutionSection> getExecutionSections() {
    return new ArrayList<>(Arrays.asList(GitCloneStepInfo.builder()
                                             .gitClone(GitCloneStepInfo.GitClone.builder().branch("master").build())
                                             .identifier("git-1")
                                             .build(),
        GitCloneStepInfo.builder()
            .gitClone(GitCloneStepInfo.GitClone.builder().branch("feature/f1").build())
            .identifier("git-2")
            .build(),
        PublishStepInfo.builder()
            .publishArtifacts(asList(DockerFileArtifact.builder()
                                         .connector(GcrConnector.builder()
                                                        .connector("gcr-connector")
                                                        .location("us.gcr.io/ci-play/portal:v01")
                                                        .build())
                                         .tag("v01")
                                         .image("ci-play/portal")
                                         .context("~/")
                                         .dockerFile("~/Dockerfile")
                                         .build()))
            .identifier("publish-1")
            .build()));
  }

  public Set<String> getPublishArtifactConnectorIds() {
    Set<String> ids = new HashSet<>();
    ids.add("gcr-connector");
    return ids;
  };

  public CIPipeline getCIPipeline() {
    return CIPipeline.builder().identifier("testPipelineIdentifier").stages(getStages()).build();
  }

  private List<StageWrapper> getStages() {
    return new ArrayList<>(Collections.singletonList(getIntegrationStage()));
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
  public Artifact getArtifact() {
    return Artifact.builder()
        .artifactStream(DockerHubArtifactStreamYaml.builder()
                            .spec(DockerHubArtifactStreamYaml.Spec.builder()
                                      .dockerhubConnector("testDCHubConnector")
                                      .imagePath("/test/path/")
                                      .tag("dch_tag")
                                      .build())
                            .type("docker-hub")
                            .build())
        .identifier("testArtifact")
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
  public IntegrationStage getIntegrationStage() {
    return IntegrationStage.builder()
        .identifier("intStageIdentifier")
        .ci(IntegrationStage.Integration.builder()
                .execution(getExecution())
                .gitConnector(getConnector())
                .container(getContainer())
                .infrastructure(getInfrastructure())
                .customVariables(getCustomVariables())
                .build())

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

  public Map<String, EncryptedDataDetail> getEncryptedSecrets() {
    Map<String, EncryptedDataDetail> envVars = new HashMap<>();
    envVars.put("VAR1", EncryptedDataDetail.builder().build());
    envVars.put("VAR2", EncryptedDataDetail.builder().build());
    return envVars;
  }
}
