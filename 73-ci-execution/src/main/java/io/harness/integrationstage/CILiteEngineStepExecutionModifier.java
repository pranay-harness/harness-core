package io.harness.integrationstage;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;

import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.environment.pod.PodSetupInfo;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.environment.pod.container.ContainerImageDetails;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.yaml.extended.CustomVariables;
import io.harness.beans.yaml.extended.connector.GitConnectorYaml;
import io.harness.exception.InvalidRequestException;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.yaml.core.Execution;
import io.harness.yaml.core.auxiliary.intfc.ExecutionSection;
import io.harness.yaml.core.intfc.Stage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ci.pod.CIContainerType;
import software.wings.beans.ci.pod.ContainerResourceParams;
import software.wings.beans.container.ImageDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Modifies saved integration stage execution plan by appending pre and post execution steps for setting up pod and
 * adding cleanup
 */

@Value
@Builder
@AllArgsConstructor
@Slf4j
public class CILiteEngineStepExecutionModifier implements StageExecutionModifier {
  private static final String LITE_ENGINE_TASK = "liteEngineTask";
  private static final String CONTAINER_NAME = "build-setup";
  private static final String CLEANUP_STEP_NAME = "cleanupStep";
  private static final String IMAGE_PATH_SPLIT_REGEX = ":";
  private String podName;

  @Override
  public Execution modifyExecutionPlan(Execution execution, Stage stage) {
    IntegrationStage integrationStage = (IntegrationStage) stage;
    return getCILiteEngineTaskExecution(integrationStage);
  }

  private Execution getCILiteEngineTaskExecution(IntegrationStage integrationStage) {
    // TODO Only git is supported currently
    if (integrationStage.getCi().getGitConnector().getType().equals("git")) {
      GitConnectorYaml gitConnectorYaml = (GitConnectorYaml) integrationStage.getCi().getGitConnector();
      return Execution.builder()
          .steps(singletonList(LiteEngineTaskStepInfo.builder()
                                   .identifier(LITE_ENGINE_TASK)
                                   .envSetup(LiteEngineTaskStepInfo.EnvSetupInfo.builder()
                                                 .gitConnectorIdentifier(gitConnectorYaml.getIdentifier())
                                                 .branchName(getBranchName(integrationStage))
                                                 .buildJobEnvInfo(getCIBuildJobEnvInfo(integrationStage))
                                                 .steps(integrationStage.getCi().getExecution())
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

  private BuildJobEnvInfo getCIBuildJobEnvInfo(IntegrationStage integrationStage) {
    // TODO Only kubernetes is supported currently
    if (integrationStage.getCi().getInfrastructure().getType().equals("kubernetes-direct")) {
      return K8BuildJobEnvInfo.builder()
          .podsSetupInfo(getCIPodsSetupInfo(integrationStage))
          .workDir(integrationStage.getCi().getWorkingDirectory())
          .publishStepConnectorIdentifier(getPublishStepConnectorIdentifier(integrationStage))
          .build();
    } else {
      throw new IllegalArgumentException("Input infrastructure type is not of type kubernetes");
    }
  }

  private Set<String> getPublishStepConnectorIdentifier(IntegrationStage integrationStage) {
    List<ExecutionSection> executionSections = integrationStage.getCi().getExecution().getSteps();
    if (isEmpty(executionSections)) {
      return Collections.emptySet();
    }

    return executionSections.stream()
        .filter(executionSection -> executionSection instanceof PublishStepInfo)
        .map(step -> ((PublishStepInfo) step).getPublishArtifacts())
        .flatMap(Collection::stream)
        .map(artifact -> artifact.getConnector().getConnector())
        .collect(Collectors.toSet());
  }

  private K8BuildJobEnvInfo.PodsSetupInfo getCIPodsSetupInfo(IntegrationStage integrationStage) {
    List<PodSetupInfo> pods = new ArrayList<>();
    pods.add(PodSetupInfo.builder()
                 .podSetupParams(
                     PodSetupInfo.PodSetupParams.builder()
                         .containerDefinitionInfos(singletonList(
                             ContainerDefinitionInfo.builder()
                                 .containerResourceParams(getContainerResourceParams(integrationStage))
                                 .envVars(getEnvVariables(integrationStage))
                                 .encryptedSecrets(getSecretVariables(integrationStage))
                                 .containerImageDetails(
                                     ContainerImageDetails.builder()
                                         .imageDetails(getImageDetails(integrationStage))
                                         .connectorIdentifier(integrationStage.getCi().getContainer().getConnector())
                                         .build())
                                 .containerType(CIContainerType.STEP_EXECUTOR)
                                 .name(CONTAINER_NAME)
                                 .build()))
                         .build())
                 .name(podName)
                 .build());
    return K8BuildJobEnvInfo.PodsSetupInfo.builder().podSetupInfoList(pods).build();
  }

  private Map<String, EncryptedDataDetail> getSecretVariables(IntegrationStage integrationStage) {
    if (isEmpty(integrationStage.getCi().getCustomVariables())) {
      return Collections.EMPTY_MAP;
    }

    return integrationStage.getCi()
        .getCustomVariables()
        .stream()
        .filter(customVariables -> {
          return customVariables.getType().equals("secret");
        }) // Todo instead of hard coded secret use variable type once we have type in cdng
        .collect(toMap(CustomVariables::getName,
            customVariables
            -> EncryptedDataDetail.builder().build())); // Todo Empty EncryptedDataDetail has to be replaced with
                                                        // encrypted values once cdng secret apis are ready
  }

  private Map<String, String> getEnvVariables(IntegrationStage integrationStage) {
    if (isEmpty(integrationStage.getCi().getCustomVariables())) {
      return Collections.EMPTY_MAP;
    }

    return integrationStage.getCi()
        .getCustomVariables()
        .stream()
        .filter(customVariables -> {
          return customVariables.getType().equals("text");
        }) // Todo instead of hard coded text use variable type once we have type in cdng
        .collect(toMap(CustomVariables::getName, CustomVariables::getValue));
  }

  private ContainerResourceParams getContainerResourceParams(IntegrationStage integrationStage) {
    return ContainerResourceParams.builder()
        .resourceRequestMilliCpu(integrationStage.getCi().getContainer().getResources().getReserve().getCpu())
        .resourceRequestMemoryMiB(integrationStage.getCi().getContainer().getResources().getReserve().getMemory())
        .resourceLimitMilliCpu(integrationStage.getCi().getContainer().getResources().getLimit().getCpu())
        .resourceLimitMemoryMiB(integrationStage.getCi().getContainer().getResources().getLimit().getMemory())
        .build();
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

    return ImageDetails.builder().name(name).tag(tag).build();
  }
}
