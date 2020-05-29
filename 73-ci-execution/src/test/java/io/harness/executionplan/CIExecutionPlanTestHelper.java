package io.harness.executionplan;

import static java.util.Arrays.asList;

import com.google.inject.Singleton;

import graph.StepInfoGraph;
import io.harness.beans.CIPipeline;
import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.environment.pod.PodSetupInfo;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.script.ScriptInfo;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.steps.stepinfo.BuildEnvSetupStepInfo;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.beans.steps.stepinfo.TestStepInfo;
import io.harness.beans.yaml.extended.artifact.DockerHubArtifactStreamYaml;
import io.harness.beans.yaml.extended.connector.GitConnectorYaml;
import io.harness.beans.yaml.extended.container.Container;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.yaml.core.Artifact;
import io.harness.yaml.core.Execution;
import io.harness.yaml.core.auxiliary.intfc.ExecutionSection;
import io.harness.yaml.core.intfc.Connector;
import io.harness.yaml.core.intfc.Infrastructure;
import io.harness.yaml.core.intfc.Stage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

  public StepInfoGraph getStepsGraph() {
    return StepInfoGraph.builder()
        .steps(asList(getBuildEnvSetupStepInfo(),
            TestStepInfo.builder().identifier(BUILD_STAGE_NAME).scriptInfos(getBuildCommandSteps()).build()))
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
  private List<ExecutionSection> getExecutionSections() {
    return new ArrayList<>(Arrays.asList(GitCloneStepInfo.builder().identifier("git-1").build(),
        GitCloneStepInfo.builder().identifier("git-2").build()));
  }

  public CIPipeline getCIPipeline() {
    return CIPipeline.builder().stages(getStages()).build();
  }

  private List<Stage> getStages() {
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
    return Container.builder().connector("testConnector").identifier("testContainer").imagePath("/test/path").build();
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
        .type("kubernetes-gcp")
        .spec(K8sDirectInfraYaml.Spec.builder().k8sConnector("testGcpConnector").namespace("testNamespace").build())
        .build();
  }
  public IntegrationStage getIntegrationStage() {
    return IntegrationStage.builder()
        .execution(getExecution())
        .connector(getConnector())
        .container(getContainer())
        .artifact(getArtifact())
        .infrastructure(getInfrastructure())
        .type("integration")
        .build();
  }
}
