package io.harness.integrationstage;

import static java.util.Arrays.asList;

import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.environment.pod.PodSetupInfo;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.environment.pod.container.ContainerImageDetails;
import io.harness.beans.environment.pod.container.ContainerType;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.steps.stepinfo.BuildEnvSetupStepInfo;
import io.harness.beans.steps.stepinfo.CleanupStepInfo;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.beans.yaml.extended.connector.GitConnectorYaml;
import io.harness.exception.InvalidRequestException;
import io.harness.yaml.core.Execution;
import io.harness.yaml.core.auxiliary.intfc.ExecutionSection;
import io.harness.yaml.core.intfc.Stage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ci.pod.ContainerResourceParams;
import software.wings.beans.container.ImageDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Modifies saved integration stage execution plan by appending pre and post execution steps for setting up pod and
 * adding cleanup
 */

@Value
@Builder
@AllArgsConstructor
@Slf4j
public class IntegrationStageExecutionModifier implements StageExecutionModifier {
  private static final String ENV_SETUP_NAME = "envSetupName";
  private static final String CONTAINER_NAME = "build-setup";
  private static final String CLEANUP_STEP_NAME = "cleanupStep";
  private static final String IMAGE_PATH_SPLIT_REGEX = ":";
  private String podName;
  private static final ImageDetails imageDetails = ImageDetails.builder()
                                                       .name("maven")
                                                       .tag("3.6.3-jdk-8")
                                                       .registryUrl("https://index.docker.io/v1/")
                                                       .username("harshjain12")
                                                       .build();

  @Override
  public Execution modifyExecutionPlan(Execution execution, Stage stage) {
    Execution modifiedExecutionPlan = Execution.builder().steps(execution.getSteps()).build();
    IntegrationStage integrationStage = (IntegrationStage) stage;
    modifiedExecutionPlan.getSteps().addAll(0, getPreIntegrationExecution(integrationStage).getSteps());
    modifiedExecutionPlan.getSteps().addAll(
        execution.getSteps().size(), getPostIntegrationSteps(integrationStage).getSteps());
    return modifiedExecutionPlan;
  }

  private Execution getPreIntegrationExecution(IntegrationStage integrationStage) {
    // TODO Only git is supported currently
    if (integrationStage.getCi().getConnector().getType().equals("git")) {
      GitConnectorYaml gitConnectorYaml = (GitConnectorYaml) integrationStage.getCi().getConnector();
      return Execution.builder()
          .steps(asList(BuildEnvSetupStepInfo.builder()
                            .identifier(ENV_SETUP_NAME)
                            .setupEnv(BuildEnvSetupStepInfo.BuildEnvSetup.builder()
                                          .gitConnectorIdentifier(gitConnectorYaml.getIdentifier())
                                          .branchName(getBranchName(integrationStage))
                                          .buildJobEnvInfo(getCIBuildJobEnvInfo(integrationStage))
                                          .build())
                            .build()))
          .build();
    } else {
      throw new IllegalArgumentException("Input connector type is not of type git");
    }
  }

  private String getBranchName(IntegrationStage integrationStage) {
    Optional<ExecutionSection> executionSection = integrationStage.getCi()
                                                      .getExecution()
                                                      .getSteps()
                                                      .stream()
                                                      .filter(section -> section instanceof GitCloneStepInfo)
                                                      .findFirst();
    if (executionSection.isPresent()) {
      GitCloneStepInfo gitCloneStepInfo = (GitCloneStepInfo) executionSection.get();
      return gitCloneStepInfo.getGitClone().getBranch();
    } else {
      throw new InvalidRequestException("Failed to execute pipeline, Git clone section is missing");
    }
  }

  private Execution getPostIntegrationSteps(IntegrationStage integrationStage) {
    return Execution.builder().steps(asList(CleanupStepInfo.builder().identifier(CLEANUP_STEP_NAME).build())).build();
  }

  private BuildJobEnvInfo getCIBuildJobEnvInfo(IntegrationStage integrationStage) {
    // TODO Only kubernetes is supported currently
    if (integrationStage.getCi().getInfrastructure().getType().equals("kubernetes-direct")) {
      return K8BuildJobEnvInfo.builder()
          .podsSetupInfo(getCIPodsSetupInfo(integrationStage))
          .workDir(integrationStage.getCi().getWorkingDirectory())
          .build();
    } else {
      throw new IllegalArgumentException("Input infrastructure type is not of type kubernetes");
    }
  }

  private K8BuildJobEnvInfo.PodsSetupInfo getCIPodsSetupInfo(IntegrationStage integrationStage) {
    ContainerResourceParams containerResourceParams =
        ContainerResourceParams.builder()
            .resourceRequestMilliCpu(integrationStage.getCi().getContainer().getResources().getReserve().getCpu())
            .resourceRequestMemoryMiB(integrationStage.getCi().getContainer().getResources().getReserve().getMemory())
            .resourceLimitMilliCpu(integrationStage.getCi().getContainer().getResources().getLimit().getCpu())
            .resourceLimitMemoryMiB(integrationStage.getCi().getContainer().getResources().getLimit().getMemory())
            .build();

    List<PodSetupInfo> pods = new ArrayList<>();
    pods.add(PodSetupInfo.builder()
                 .podSetupParams(
                     PodSetupInfo.PodSetupParams.builder()
                         .containerDefinitionInfos(asList(
                             ContainerDefinitionInfo.builder()
                                 .containerResourceParams(containerResourceParams)
                                 .containerImageDetails(
                                     ContainerImageDetails.builder()
                                         .imageDetails(getImageDetails(integrationStage))
                                         .connectorIdentifier(integrationStage.getCi().getArtifact().getIdentifier())
                                         .build())
                                 .containerType(ContainerType.STEP_EXECUTOR)
                                 .name(CONTAINER_NAME)
                                 .build()))
                         .build())
                 .name(podName)
                 .build());
    return K8BuildJobEnvInfo.PodsSetupInfo.builder().podSetupInfoList(pods).build();
  }

  private ImageDetails getImageDetails(IntegrationStage integrationStage) {
    String imagePath = integrationStage.getCi().getContainer().getImagePath();
    String name = integrationStage.getCi().getContainer().getImagePath();
    String tag = "latest";

    if (imagePath.contains(IMAGE_PATH_SPLIT_REGEX)) {
      String[] subTokens = integrationStage.getCi().getContainer().getImagePath().split(IMAGE_PATH_SPLIT_REGEX);
      if (subTokens.length == 2) {
        name = subTokens[0];
        tag = subTokens[1];
      } else {
        throw new InvalidRequestException("ImagePath should not contain multiple tags");
      }
    }

    return ImageDetails.builder()
        .name(name)
        .tag(tag)
        .registryUrl("https://index.docker.io/v1/")
        .username("harshjain12")
        .build();
  }
}
