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
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.BuildEnvSetupStepInfo;
import io.harness.beans.steps.stepinfo.CleanupStepInfo;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.yaml.extended.CustomVariables;
import io.harness.beans.yaml.extended.connector.GitConnectorYaml;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.model.ImageDetails;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import io.harness.yaml.core.intfc.Stage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.ci.pod.CIContainerType;
import software.wings.beans.ci.pod.ContainerResourceParams;
import software.wings.beans.ci.pod.EncryptedVariableWithType;

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
public class IntegrationStageExecutionModifier implements StageExecutionModifier {
  private static final String ENV_SETUP_NAME = "envSetupName";
  private static final String CONTAINER_NAME = "build-setup";
  private static final String CLEANUP_STEP_NAME = "cleanupStep";
  private static final String IMAGE_PATH_SPLIT_REGEX = ":";
  private String podName;

  @Override
  public ExecutionElement modifyExecutionPlan(ExecutionElement execution, Stage stage) {
    IntegrationStage integrationStage = (IntegrationStage) stage;
    List<ExecutionWrapper> steps = execution.getSteps();
    steps.addAll(0, getPreIntegrationExecution(integrationStage).getSteps());
    steps.addAll(steps.size(), getPostIntegrationSteps().getSteps());
    return ExecutionElement.builder().steps(steps).build();
  }

  private ExecutionElement getPreIntegrationExecution(IntegrationStage integrationStage) {
    // TODO Only git is supported currently
    if (integrationStage.getGitConnector().getType().equals("git")) {
      GitConnectorYaml gitConnectorYaml = (GitConnectorYaml) integrationStage.getGitConnector();
      return ExecutionElement.builder()
          .steps(singletonList(StepElement.builder()
                                   .identifier(ENV_SETUP_NAME)
                                   .stepSpecType(BuildEnvSetupStepInfo.builder()
                                                     .identifier(ENV_SETUP_NAME)
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
    Optional<CIStepInfo> stepInfo =
        integrationStage.getExecution()
            .getSteps()
            .stream()
            .filter(executionWrapper -> executionWrapper instanceof StepElement)
            .filter(executionWrapper -> ((StepElement) executionWrapper).getStepSpecType() instanceof GitCloneStepInfo)
            .findFirst()
            .map(executionWrapper -> (GitCloneStepInfo) ((StepElement) executionWrapper).getStepSpecType());
    if (stepInfo.isPresent()) {
      GitCloneStepInfo gitCloneStepInfo = (GitCloneStepInfo) stepInfo.get();
      return gitCloneStepInfo.getBranch();
    } else {
      throw new InvalidRequestException("Failed to execute pipeline, Git clone section is missing");
    }
  }

  private ExecutionElement getPostIntegrationSteps() {
    return ExecutionElement.builder()
        .steps(singletonList(StepElement.builder()
                                 .identifier(CLEANUP_STEP_NAME)
                                 .stepSpecType(CleanupStepInfo.builder().identifier(CLEANUP_STEP_NAME).build())
                                 .build()))
        .build();
  }

  private BuildJobEnvInfo getCIBuildJobEnvInfo(IntegrationStage integrationStage) {
    // TODO Only kubernetes is supported currently
    if (integrationStage.getInfrastructure().getType().equals("kubernetes-direct")) {
      return K8BuildJobEnvInfo.builder()
          .podsSetupInfo(getCIPodsSetupInfo(integrationStage))
          .workDir(integrationStage.getWorkingDirectory())
          .publishStepConnectorIdentifier(getPublishStepConnectorIdentifier(integrationStage))
          .build();
    } else {
      throw new IllegalArgumentException("Input infrastructure type is not of type kubernetes");
    }
  }

  private Set<String> getPublishStepConnectorIdentifier(IntegrationStage integrationStage) {
    List<ExecutionWrapper> executionWrappers = integrationStage.getExecution().getSteps();
    if (isEmpty(executionWrappers)) {
      return Collections.emptySet();
    }

    return executionWrappers.stream()
        .filter(executionSection -> executionSection instanceof StepElement)
        .filter(executionSection -> ((StepElement) executionSection).getStepSpecType() instanceof PublishStepInfo)
        .map(step -> ((PublishStepInfo) ((StepElement) step).getStepSpecType()).getPublishArtifacts())
        .flatMap(Collection::stream)
        .map(artifact -> artifact.getConnector().getConnector())
        .collect(Collectors.toSet());
  }

  private K8BuildJobEnvInfo.PodsSetupInfo getCIPodsSetupInfo(IntegrationStage integrationStage) {
    List<PodSetupInfo> pods = new ArrayList<>();
    pods.add(
        PodSetupInfo.builder()
            .podSetupParams(PodSetupInfo.PodSetupParams.builder()
                                .containerDefinitionInfos(singletonList(
                                    ContainerDefinitionInfo.builder()
                                        .containerResourceParams(getContainerResourceParams(integrationStage))
                                        .envVars(getEnvVariables(integrationStage))
                                        .encryptedSecrets(getSecretVariables(integrationStage))
                                        .containerImageDetails(
                                            ContainerImageDetails.builder()
                                                .imageDetails(getImageDetails(integrationStage))
                                                .connectorIdentifier(integrationStage.getContainer().getConnector())
                                                .build())
                                        .containerType(CIContainerType.STEP_EXECUTOR)
                                        .name(CONTAINER_NAME)
                                        .build()))
                                .build())
            .name(podName)
            .build());
    return K8BuildJobEnvInfo.PodsSetupInfo.builder().podSetupInfoList(pods).build();
  }

  private Map<String, EncryptedVariableWithType> getSecretVariables(IntegrationStage integrationStage) {
    if (isEmpty(integrationStage.getCustomVariables())) {
      return Collections.emptyMap();
    }

    return integrationStage.getCustomVariables()
        .stream()
        .filter(customVariables
            -> customVariables.getType().equals(
                "secret")) // Todo instead of hard coded secret use variable type once we have type in cdng
        .collect(toMap(CustomVariables::getName,
            customVariables
            -> EncryptedVariableWithType.builder().build())); // Todo Empty EncryptedDataDetail has to be replaced with
                                                              // encrypted values once cdng secret apis are ready
  }

  private Map<String, String> getEnvVariables(IntegrationStage integrationStage) {
    if (isEmpty(integrationStage.getCustomVariables())) {
      return Collections.emptyMap();
    }

    return integrationStage.getCustomVariables()
        .stream()
        .filter(customVariables
            -> customVariables.getType().equals(
                "text")) // Todo instead of hard coded text use variable type once we have type in cdng
        .collect(toMap(CustomVariables::getName, CustomVariables::getValue));
  }

  private ContainerResourceParams getContainerResourceParams(IntegrationStage integrationStage) {
    return ContainerResourceParams.builder()
        .resourceRequestMilliCpu(integrationStage.getContainer().getResources().getReserve().getCpu())
        .resourceRequestMemoryMiB(integrationStage.getContainer().getResources().getReserve().getMemory())
        .resourceLimitMilliCpu(integrationStage.getContainer().getResources().getLimit().getCpu())
        .resourceLimitMemoryMiB(integrationStage.getContainer().getResources().getLimit().getMemory())
        .build();
  }

  private ImageDetails getImageDetails(IntegrationStage integrationStage) {
    String imagePath = integrationStage.getContainer().getImagePath();
    String name = integrationStage.getContainer().getImagePath();
    String tag = "latest";

    if (imagePath.contains(IMAGE_PATH_SPLIT_REGEX)) {
      String[] subTokens = integrationStage.getContainer().getImagePath().split(IMAGE_PATH_SPLIT_REGEX);
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
