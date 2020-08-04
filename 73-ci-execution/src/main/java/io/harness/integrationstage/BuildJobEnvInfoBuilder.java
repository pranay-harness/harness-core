package io.harness.integrationstage;

import static io.harness.common.CIExecutionConstants.PORT_STARTING_RANGE;
import static io.harness.common.CIExecutionConstants.PVC_DEFAULT_STORAGE_CLASS;
import static io.harness.common.CIExecutionConstants.PVC_DEFAULT_STORAGE_SIZE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static software.wings.common.CICommonPodConstants.POD_NAME;
import static software.wings.common.CICommonPodConstants.STEP_EXEC;

import com.google.inject.Singleton;

import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.environment.pod.PodSetupInfo;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.environment.pod.container.ContainerImageDetails;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.steps.stepinfo.publish.artifact.Artifact;
import io.harness.beans.yaml.extended.CustomVariables;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.model.ImageDetails;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import software.wings.beans.ci.pod.CIContainerType;
import software.wings.beans.ci.pod.ContainerResourceParams;
import software.wings.beans.ci.pod.EncryptedVariableWithType;
import software.wings.beans.ci.pod.PVCParams;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
public class BuildJobEnvInfoBuilder {
  private static final String IMAGE_PATH_SPLIT_REGEX = ":";
  private static final String CONTAINER_NAME = "build-setup";
  private static final String LATEST_TAG = "latest";

  private static final SecureRandom random = new SecureRandom();

  public BuildJobEnvInfo getCIBuildJobEnvInfo(
      IntegrationStage integrationStage, boolean isFirstPod, String buildNumber, Integer parallelism) {
    // TODO Only kubernetes is supported currently
    if (integrationStage.getInfrastructure().getType().equals("kubernetes-direct")) {
      return K8BuildJobEnvInfo.builder()
          .podsSetupInfo(getCIPodsSetupInfo(integrationStage, parallelism, isFirstPod, buildNumber))
          .workDir(integrationStage.getWorkingDirectory())
          .publishStepConnectorIdentifier(getPublishStepConnectorIdentifier(integrationStage))
          .build();
    } else {
      throw new IllegalArgumentException("Input infrastructure type is not of type kubernetes");
    }
  }

  private K8BuildJobEnvInfo.PodsSetupInfo getCIPodsSetupInfo(
      IntegrationStage integrationStage, Integer parallelism, boolean isFirstPod, String buildNumber) {
    List<PodSetupInfo> pods = new ArrayList<>();
    String podName = generatePodName(integrationStage);

    pods.add(
        PodSetupInfo.builder()
            .podSetupParams(PodSetupInfo.PodSetupParams.builder()
                                .containerDefinitionInfos(createContainerDefinitions(parallelism, integrationStage))
                                .build())
            .name(podName)
            .pvcParams(createPVCParams(isFirstPod, buildNumber))
            .build());
    return K8BuildJobEnvInfo.PodsSetupInfo.builder().podSetupInfoList(pods).build();
  }

  private PVCParams createPVCParams(boolean isFirstPod, String buildNumber) {
    return PVCParams.builder()
        .claimName(buildNumber)
        .volumeName(STEP_EXEC)
        .isPresent(!isFirstPod)
        .sizeMib(PVC_DEFAULT_STORAGE_SIZE)
        .storageClass(PVC_DEFAULT_STORAGE_CLASS)
        .build();
  }

  private List<ContainerDefinitionInfo> createContainerDefinitions(
      Integer parallelism, IntegrationStage integrationStage) {
    int i = 1;
    List<ContainerDefinitionInfo> containerDefinitionInfos = new ArrayList<>();
    // Add container def for main lite engine
    containerDefinitionInfos.add(
        ContainerDefinitionInfo.builder()
            .containerResourceParams(getContainerResourceParams(integrationStage))
            .envVars(getEnvVariables(integrationStage))
            .encryptedSecrets(getSecretVariables(integrationStage))
            .isMainLiteEngine(true)
            .containerImageDetails(ContainerImageDetails.builder()
                                       .imageDetails(getImageDetails(integrationStage))
                                       .connectorIdentifier(integrationStage.getContainer().getConnector())
                                       .build())
            .containerType(CIContainerType.STEP_EXECUTOR)
            .name(CONTAINER_NAME + i)
            .build());
    // Add container def for worker lite engine
    while (i < parallelism) {
      containerDefinitionInfos.add(
          ContainerDefinitionInfo.builder()
              .containerResourceParams(getContainerResourceParams(integrationStage))
              .envVars(getEnvVariables(integrationStage))
              .encryptedSecrets(getSecretVariables(integrationStage))
              .isMainLiteEngine(false)
              .ports(asList(PORT_STARTING_RANGE + i))
              .containerImageDetails(ContainerImageDetails.builder()
                                         .imageDetails(getImageDetails(integrationStage))
                                         .connectorIdentifier(integrationStage.getContainer().getConnector())
                                         .build())
              .containerType(CIContainerType.STEP_EXECUTOR)
              .name(CONTAINER_NAME + (i + 1))
              .build());
      i++;
    }
    return containerDefinitionInfos;
  }

  private String generatePodName(IntegrationStage integrationStage) {
    // TODO Use better pod naming strategy after discussion with PM, attach build number in future
    return POD_NAME + "-" + integrationStage.getIdentifier() + random.nextInt(100000000);
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
            customVariables -> EncryptedVariableWithType.builder().build())); // Todo Empty EncryptedDataDetail has to
    // be replaced with
    // encrypted values once cdng secret apis are ready
  }

  private Set<String> getPublishStepConnectorIdentifier(IntegrationStage integrationStage) {
    List<ExecutionWrapper> executionWrappers = integrationStage.getExecution().getSteps();
    if (isEmpty(executionWrappers)) {
      return Collections.emptySet();
    }

    Set<String> set = new HashSet<>();
    for (ExecutionWrapper executionSection : executionWrappers) {
      if (executionSection instanceof ParallelStepElement) {
        for (ExecutionWrapper executionWrapper : ((ParallelStepElement) executionSection).getSections()) {
          if (executionWrapper instanceof StepElement) {
            StepElement stepElement = (StepElement) executionWrapper;
            if (stepElement.getStepSpecType() instanceof PublishStepInfo) {
              List<Artifact> publishArtifacts = ((PublishStepInfo) stepElement.getStepSpecType()).getPublishArtifacts();
              for (Artifact artifact : publishArtifacts) {
                String connector = artifact.getConnector().getConnector();
                set.add(connector);
              }
            }
          }
        }
      } else if (executionSection instanceof StepElement) {
        if (((StepElement) executionSection).getStepSpecType() instanceof PublishStepInfo) {
          List<Artifact> publishArtifacts =
              ((PublishStepInfo) ((StepElement) executionSection).getStepSpecType()).getPublishArtifacts();
          for (Artifact artifact : publishArtifacts) {
            set.add(artifact.getConnector().getConnector());
          }
        }
      }
    }
    return set;
  }

  private ContainerResourceParams getContainerResourceParams(IntegrationStage integrationStage) {
    return ContainerResourceParams.builder()
        .resourceRequestMilliCpu(integrationStage.getContainer().getResources().getReserve().getCpu())
        .resourceRequestMemoryMiB(integrationStage.getContainer().getResources().getReserve().getMemory())
        .resourceLimitMilliCpu(integrationStage.getContainer().getResources().getLimit().getCpu())
        .resourceLimitMemoryMiB(integrationStage.getContainer().getResources().getLimit().getMemory())
        .build();
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

  private ImageDetails getImageDetails(IntegrationStage integrationStage) {
    String imagePath = integrationStage.getContainer().getImagePath();
    String name = integrationStage.getContainer().getImagePath();
    String tag = LATEST_TAG;

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
