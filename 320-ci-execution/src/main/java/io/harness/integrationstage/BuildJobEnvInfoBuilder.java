package io.harness.integrationstage;

import static io.harness.common.CICommonPodConstants.MOUNT_PATH;
import static io.harness.common.CICommonPodConstants.STEP_EXEC;
import static io.harness.common.CIExecutionConstants.IMAGE_PATH_SPLIT_REGEX;
import static io.harness.common.CIExecutionConstants.PLUGIN_ENV_PREFIX;
import static io.harness.common.CIExecutionConstants.PORT_STARTING_RANGE;
import static io.harness.common.CIExecutionConstants.PVC_DEFAULT_STORAGE_CLASS;
import static io.harness.common.CIExecutionConstants.SHARED_VOLUME_PREFIX;
import static io.harness.common.CIExecutionConstants.STEP_PREFIX;
import static io.harness.common.CIExecutionConstants.STEP_REQUEST_MEMORY_MIB;
import static io.harness.common.CIExecutionConstants.STEP_REQUEST_MILLI_CPU;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.util.stream.Collectors.toMap;

import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.environment.K8BuildJobEnvInfo.ConnectorConversionInfo;
import io.harness.beans.environment.pod.PodSetupInfo;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.environment.pod.container.ContainerImageDetails;
import io.harness.beans.executionargs.CIExecutionArgs;
import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.TestIntelligenceStepInfo;
import io.harness.beans.steps.stepinfo.publish.artifact.Artifact;
import io.harness.beans.yaml.extended.CustomSecretVariable;
import io.harness.beans.yaml.extended.CustomTextVariable;
import io.harness.beans.yaml.extended.CustomVariable;
import io.harness.beans.yaml.extended.container.ContainerResource;
import io.harness.beans.yaml.extended.container.quantity.unit.BinaryQuantityUnit;
import io.harness.beans.yaml.extended.container.quantity.unit.DecimalQuantityUnit;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.ci.config.CIExecutionServiceConfig;
import io.harness.ci.utils.QuantityUtils;
import io.harness.common.CICommonPodConstants;
import io.harness.delegate.beans.ci.pod.CIContainerType;
import io.harness.delegate.beans.ci.pod.ContainerResourceParams;
import io.harness.delegate.beans.ci.pod.EnvVariableEnum;
import io.harness.delegate.beans.ci.pod.PVCParams;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.model.ImageDetails;
import io.harness.stateutils.buildstate.PluginSettingUtils;
import io.harness.stateutils.buildstate.providers.StepContainerUtils;
import io.harness.util.PortFinder;
import io.harness.utils.IdentifierRefHelper;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class BuildJobEnvInfoBuilder {
  private static final String USERNAME_PREFIX = "USERNAME_";
  private static final String PASSW_PREFIX = "PASSWORD_";
  private static final String ENDPOINT_PREFIX = "ENDPOINT_";
  private static final String ACCESS_KEY_PREFIX = "ACCESS_KEY_";
  private static final String SECRET_KEY_PREFIX = "SECRET_KEY_";
  private static final String SECRET_PATH_PREFIX = "SECRET_PATH_";

  private static final String PLUGIN_USERNAME = "PLUGIN_USERNAME";
  private static final String PLUGIN_PASSW = "PLUGIN_PASSWORD";
  private static final String PLUGIN_REGISTRY = "PLUGIN_REGISTRY";
  private static final String PLUGIN_ACCESS_KEY = "PLUGIN_ACCESS_KEY";
  private static final String PLUGIN_SECRET_KEY = "PLUGIN_SECRET_KEY";
  private static final String PLUGIN_JSON_KEY = "PLUGIN_JSON_KEY";

  @Inject private CIExecutionServiceConfig ciExecutionServiceConfig;

  public BuildJobEnvInfo getCIBuildJobEnvInfo(IntegrationStage integrationStage, CIExecutionArgs ciExecutionArgs,
      List<ExecutionWrapper> steps, boolean isFirstPod, String podName) {
    log.info("Creating CI Build Job info for integration stage {} and pod name {}", integrationStage.getIdentifier(),
        podName);
    // TODO Only kubernetes is supported currently
    if (integrationStage.getInfrastructure() == null) {
      throw new IllegalArgumentException("Input infrastructure is not set");
    }
    if (integrationStage.getInfrastructure().getType() == Infrastructure.Type.KUBERNETES_DIRECT) {
      return K8BuildJobEnvInfo.builder()
          .podsSetupInfo(getCIPodsSetupInfo(integrationStage, ciExecutionArgs, steps, isFirstPod, podName))
          .workDir(getWorkingDirectory(integrationStage))
          .publishArtifactStepIds(getPublishArtifactStepIds(integrationStage))
          .stepConnectorRefs(getStepConnectorRefs(integrationStage))
          .build();
    } else {
      throw new IllegalArgumentException("Input infrastructure type is not of type kubernetes");
    }
  }

  private K8BuildJobEnvInfo.PodsSetupInfo getCIPodsSetupInfo(IntegrationStage integrationStage,
      CIExecutionArgs ciExecutionArgs, List<ExecutionWrapper> steps, boolean isFirstPod, String podName) {
    List<PodSetupInfo> pods = new ArrayList<>();

    Set<Integer> usedPorts = new HashSet<>();
    PortFinder portFinder = PortFinder.builder().startingPort(PORT_STARTING_RANGE).usedPorts(usedPorts).build();
    String workDirPath = getWorkingDirectoryPath(getWorkingDirectory(integrationStage));
    List<ContainerDefinitionInfo> serviceContainerDefinitionInfos =
        CIServiceBuilder.createServicesContainerDefinition(integrationStage, portFinder, ciExecutionServiceConfig);
    List<ContainerDefinitionInfo> stepContainerDefinitionInfos =
        createStepsContainerDefinition(steps, integrationStage, ciExecutionArgs, portFinder);

    List<ContainerDefinitionInfo> containerDefinitionInfos = new ArrayList<>();
    containerDefinitionInfos.addAll(serviceContainerDefinitionInfos);
    containerDefinitionInfos.addAll(stepContainerDefinitionInfos);

    Integer stageMemoryRequest = getStageMemoryRequest(steps);
    Integer stageCpuRequest = getStageCpuRequest(steps);
    List<String> serviceIdList = CIServiceBuilder.getServiceIdList(integrationStage);
    List<Integer> serviceGrpcPortList = CIServiceBuilder.getServiceGrpcPortList(integrationStage);
    Map<String, String> volumeToMountPath = getVolumeToMountPath(integrationStage.getSharedPaths());
    pods.add(PodSetupInfo.builder()
                 .podSetupParams(
                     PodSetupInfo.PodSetupParams.builder().containerDefinitionInfos(containerDefinitionInfos).build())
                 .name(podName)
                 .pvcParamsList(createPVCParams(isFirstPod, podName, volumeToMountPath))
                 .volumeToMountPath(volumeToMountPath)
                 .stageCpuRequest(stageCpuRequest)
                 .stageMemoryRequest(stageMemoryRequest)
                 .serviceIdList(serviceIdList)
                 .serviceGrpcPortList(serviceGrpcPortList)
                 .workDirPath(workDirPath)
                 .build());
    return K8BuildJobEnvInfo.PodsSetupInfo.builder().podSetupInfoList(pods).build();
  }

  private List<PVCParams> createPVCParams(boolean isFirstPod, String podName, Map<String, String> volumeToMountPath) {
    List<PVCParams> pvcParamsList = new ArrayList<>();

    for (String volumeName : volumeToMountPath.keySet()) {
      // TODO: Fix the claim name to make it unique for different stages in a build and same build number across
      // different account
      String claimName = String.format("%s-%s", podName, volumeName);
      pvcParamsList.add(PVCParams.builder()
                            .claimName(claimName)
                            .volumeName(volumeName)
                            .isPresent(!isFirstPod)
                            .sizeMib(ciExecutionServiceConfig.getPvcDefaultStorageSize())
                            .storageClass(PVC_DEFAULT_STORAGE_CLASS)
                            .build());
    }
    return pvcParamsList;
  }

  private Map<String, String> getVolumeToMountPath(List<String> sharedPaths) {
    Map<String, String> volumeToMountPath = new HashMap<>();
    volumeToMountPath.put(STEP_EXEC, MOUNT_PATH);

    if (sharedPaths != null) {
      int index = 0;
      for (String path : sharedPaths) {
        String volumeName = String.format("%s%d", SHARED_VOLUME_PREFIX, index);
        volumeToMountPath.put(volumeName, path);
        index++;
      }
    }
    return volumeToMountPath;
  }

  private List<ContainerDefinitionInfo> createStepsContainerDefinition(List<ExecutionWrapper> steps,
      IntegrationStage integrationStage, CIExecutionArgs ciExecutionArgs, PortFinder portFinder) {
    List<ContainerDefinitionInfo> containerDefinitionInfos = new ArrayList<>();
    if (steps == null) {
      return containerDefinitionInfos;
    }

    int stepIndex = 0;
    for (ExecutionWrapper executionWrapper : steps) {
      if (executionWrapper instanceof StepElement) {
        stepIndex++;
        ContainerDefinitionInfo containerDefinitionInfo = createStepContainerDefinition(
            (StepElement) executionWrapper, integrationStage, ciExecutionArgs, portFinder, stepIndex);
        if (containerDefinitionInfo != null) {
          containerDefinitionInfos.add(containerDefinitionInfo);
        }
      } else if (executionWrapper instanceof ParallelStepElement) {
        ParallelStepElement parallel = (ParallelStepElement) executionWrapper;
        if (parallel.getSections() != null) {
          for (ExecutionWrapper executionWrapperInParallel : parallel.getSections()) {
            if (!(executionWrapperInParallel instanceof StepElement)) {
              continue;
            }

            stepIndex++;
            ContainerDefinitionInfo containerDefinitionInfo = createStepContainerDefinition(
                (StepElement) executionWrapperInParallel, integrationStage, ciExecutionArgs, portFinder, stepIndex);
            if (containerDefinitionInfo != null) {
              containerDefinitionInfos.add(containerDefinitionInfo);
            }
          }
        }
      }
    }
    return containerDefinitionInfos;
  }

  private ContainerDefinitionInfo createStepContainerDefinition(StepElement stepElement,
      IntegrationStage integrationStage, CIExecutionArgs ciExecutionArgs, PortFinder portFinder, int stepIndex) {
    if (!(stepElement.getStepSpecType() instanceof CIStepInfo)) {
      return null;
    }

    CIStepInfo ciStepInfo = (CIStepInfo) stepElement.getStepSpecType();
    switch (ciStepInfo.getNonYamlInfo().getStepInfoType()) {
      case RUN:
        return createRunStepContainerDefinition(
            (RunStepInfo) ciStepInfo, integrationStage, ciExecutionArgs, portFinder, stepIndex);
      case DOCKER:
      case ECR:
      case GCR:
      case SAVE_CACHE_S3:
      case RESTORE_CACHE_S3:
      case RESTORE_CACHE_GCS:
      case SAVE_CACHE_GCS:
      case UPLOAD_S3:
      case UPLOAD_GCS:
        return createPluginCompatibleStepContainerDefinition(
            (PluginCompatibleStep) ciStepInfo, ciExecutionArgs, portFinder, stepIndex);
      case PLUGIN:
        return createPluginStepContainerDefinition((PluginStepInfo) ciStepInfo, ciExecutionArgs, portFinder, stepIndex);
      case TEST_INTELLIGENCE:
        return createTestIntelligenceStepContainerDefinition(
            (TestIntelligenceStepInfo) ciStepInfo, integrationStage, ciExecutionArgs, portFinder, stepIndex);
      default:
        return null;
    }
  }

  private ContainerDefinitionInfo createPluginCompatibleStepContainerDefinition(
      PluginCompatibleStep stepInfo, CIExecutionArgs ciExecutionArgs, PortFinder portFinder, int stepIndex) {
    Integer port = portFinder.getNextPort();
    stepInfo.setPort(port);

    String containerName = String.format("%s%d", STEP_PREFIX, stepIndex);
    Map<String, String> envVarMap = new HashMap<>();
    envVarMap.putAll(BuildEnvironmentUtils.getBuildEnvironmentVariables(ciExecutionArgs));
    envVarMap.putAll(PluginSettingUtils.getPluginCompatibleEnvVariables(stepInfo));
    return ContainerDefinitionInfo.builder()
        .name(containerName)
        .commands(StepContainerUtils.getCommand())
        .args(StepContainerUtils.getArguments(port))
        .envVars(envVarMap)
        .containerImageDetails(ContainerImageDetails.builder().imageDetails(getImageInfo(stepInfo.getImage())).build())
        .containerResourceParams(getStepContainerResource(stepInfo.getResources()))
        .ports(Collections.singletonList(port))
        .containerType(CIContainerType.PLUGIN)
        .stepIdentifier(stepInfo.getIdentifier())
        .stepName(stepInfo.getName())
        .build();
  }

  private ContainerDefinitionInfo createRunStepContainerDefinition(RunStepInfo runStepInfo,
      IntegrationStage integrationStage, CIExecutionArgs ciExecutionArgs, PortFinder portFinder, int stepIndex) {
    Integer port = portFinder.getNextPort();
    runStepInfo.setPort(port);

    String containerName = String.format("%s%d", STEP_PREFIX, stepIndex);
    Map<String, String> stepEnvVars = new HashMap<>();
    stepEnvVars.putAll(getEnvVariables(integrationStage));
    stepEnvVars.putAll(BuildEnvironmentUtils.getBuildEnvironmentVariables(ciExecutionArgs));
    if (!isEmpty(runStepInfo.getEnvironment())) {
      stepEnvVars.putAll(runStepInfo.getEnvironment());
    }
    return ContainerDefinitionInfo.builder()
        .name(containerName)
        .commands(StepContainerUtils.getCommand())
        .args(StepContainerUtils.getArguments(port))
        .envVars(stepEnvVars)
        .secretVariables(getSecretVariables(integrationStage))
        .containerImageDetails(ContainerImageDetails.builder()
                                   .imageDetails(getImageInfo(runStepInfo.getImage()))
                                   .connectorIdentifier(runStepInfo.getConnector())
                                   .build())
        .containerResourceParams(getStepContainerResource(runStepInfo.getResources()))
        .ports(Collections.singletonList(port))
        .containerType(CIContainerType.RUN)
        .stepIdentifier(runStepInfo.getIdentifier())
        .stepName(runStepInfo.getName())
        .build();
  }

  private ContainerDefinitionInfo createTestIntelligenceStepContainerDefinition(
      TestIntelligenceStepInfo testIntelligenceStepInfo, IntegrationStage integrationStage,
      CIExecutionArgs ciExecutionArgs, PortFinder portFinder, int stepIndex) {
    Integer port = portFinder.getNextPort();
    testIntelligenceStepInfo.setPort(port);

    String containerName = String.format("%s%d", STEP_PREFIX, stepIndex);
    Map<String, String> stepEnvVars = new HashMap<>();
    stepEnvVars.putAll(getEnvVariables(integrationStage));
    stepEnvVars.putAll(BuildEnvironmentUtils.getBuildEnvironmentVariables(ciExecutionArgs));

    return ContainerDefinitionInfo.builder()
        .name(containerName)
        .commands(StepContainerUtils.getCommand())
        .args(StepContainerUtils.getArguments(port))
        .envVars(stepEnvVars)
        .secretVariables(getSecretVariables(integrationStage))
        .containerImageDetails(ContainerImageDetails.builder()
                                   .imageDetails(getImageInfo(testIntelligenceStepInfo.getImage()))
                                   .connectorIdentifier(testIntelligenceStepInfo.getConnector())
                                   .build())
        .containerResourceParams(getStepContainerResource(testIntelligenceStepInfo.getResources()))
        .ports(Collections.singletonList(port))
        .containerType(CIContainerType.TEST_INTELLIGENCE)
        .build();
  }

  private ContainerDefinitionInfo createPluginStepContainerDefinition(
      PluginStepInfo pluginStepInfo, CIExecutionArgs ciExecutionArgs, PortFinder portFinder, int stepIndex) {
    Integer port = portFinder.getNextPort();
    pluginStepInfo.setPort(port);

    String containerName = String.format("%s%d", STEP_PREFIX, stepIndex);
    Map<String, String> envVarMap = new HashMap<>();
    envVarMap.putAll(BuildEnvironmentUtils.getBuildEnvironmentVariables(ciExecutionArgs));
    if (!isEmpty(pluginStepInfo.getSettings())) {
      for (Map.Entry<String, String> entry : pluginStepInfo.getSettings().entrySet()) {
        String key = PLUGIN_ENV_PREFIX + entry.getKey().toUpperCase();
        envVarMap.put(key, entry.getValue());
      }
    }
    return ContainerDefinitionInfo.builder()
        .name(containerName)
        .commands(StepContainerUtils.getCommand())
        .args(StepContainerUtils.getArguments(port))
        .envVars(envVarMap)
        .containerImageDetails(ContainerImageDetails.builder()
                                   .imageDetails(getImageInfo(pluginStepInfo.getImage()))
                                   .connectorIdentifier(pluginStepInfo.getConnector())
                                   .build())
        .containerResourceParams(getStepContainerResource(pluginStepInfo.getResources()))
        .ports(Collections.singletonList(port))
        .containerType(CIContainerType.PLUGIN)
        .stepIdentifier(pluginStepInfo.getIdentifier())
        .stepName(pluginStepInfo.getName())
        .build();
  }

  private ContainerResourceParams getStepContainerResource(ContainerResource resource) {
    return ContainerResourceParams.builder()
        .resourceRequestMilliCpu(STEP_REQUEST_MILLI_CPU)
        .resourceRequestMemoryMiB(STEP_REQUEST_MEMORY_MIB)
        .resourceLimitMilliCpu(getContainerCpuLimit(resource))
        .resourceLimitMemoryMiB(getContainerMemoryLimit(resource))
        .build();
  }

  private List<CustomSecretVariable> getSecretVariables(IntegrationStage integrationStage) {
    if (isEmpty(integrationStage.getCustomVariables())) {
      return Collections.emptyList();
    }

    return integrationStage.getCustomVariables()
        .stream()
        .filter(customVariables -> customVariables.getType().equals(CustomVariable.Type.SECRET))
        .map(customVariable -> (CustomSecretVariable) customVariable)
        .collect(Collectors.toList());
  }

  private Set<String> getPublishArtifactStepIds(IntegrationStage integrationStage) {
    List<ExecutionWrapper> executionWrappers = integrationStage.getExecution().getSteps();
    if (isEmpty(executionWrappers)) {
      return Collections.emptySet();
    }

    Set<String> set = new HashSet<>();
    for (ExecutionWrapper executionSection : executionWrappers) {
      if (executionSection instanceof ParallelStepElement) {
        for (ExecutionWrapper executionWrapper : ((ParallelStepElement) executionSection).getSections()) {
          if (executionWrapper instanceof StepElement) {
            if (((StepElement) executionWrapper).getType().equals("publishArtifacts")) {
              set.add(((PublishStepInfo) ((StepElement) executionWrapper).getStepSpecType()).getIdentifier());
            }
          }
        }
      } else if (executionSection instanceof StepElement) {
        if (((StepElement) executionSection).getType().equals("publishArtifacts")) {
          set.add(((PublishStepInfo) ((StepElement) executionSection).getStepSpecType()).getIdentifier());
        }
      }
    }
    return set;
  }

  private Map<String, ConnectorConversionInfo> getStepConnectorRefs(IntegrationStage integrationStage) {
    List<ExecutionWrapper> executionWrappers = integrationStage.getExecution().getSteps();
    if (isEmpty(executionWrappers)) {
      return Collections.emptyMap();
    }

    Map<String, ConnectorConversionInfo> map = new HashMap<>();
    for (ExecutionWrapper executionSection : executionWrappers) {
      if (executionSection instanceof ParallelStepElement) {
        for (ExecutionWrapper executionWrapper : ((ParallelStepElement) executionSection).getSections()) {
          if (executionWrapper instanceof StepElement) {
            map.putAll(getStepConnectorConversionInfo((StepElement) executionWrapper));
          }
        }
      } else if (executionSection instanceof StepElement) {
        map.putAll(getStepConnectorConversionInfo((StepElement) executionSection));
      }
    }
    return map;
  }

  private Map<String, ConnectorConversionInfo> getStepConnectorConversionInfo(StepElement stepElement) {
    Map<String, ConnectorConversionInfo> map = new HashMap<>();
    if (stepElement.getStepSpecType() instanceof PublishStepInfo) {
      List<Artifact> publishArtifacts = ((PublishStepInfo) stepElement.getStepSpecType()).getPublishArtifacts();
      for (Artifact artifact : publishArtifacts) {
        String connectorRef = artifact.getConnector().getConnectorRef();
        String connectorId = IdentifierRefHelper.getIdentifier(connectorRef);
        String stepId = stepElement.getIdentifier();
        switch (artifact.getConnector().getType()) {
          case ECR:
          case S3:
            map.put(stepId,
                ConnectorConversionInfo.builder()
                    .connectorRef(connectorRef)
                    .envToSecretEntry(EnvVariableEnum.AWS_ACCESS_KEY, ACCESS_KEY_PREFIX + connectorId)
                    .envToSecretEntry(EnvVariableEnum.AWS_SECRET_KEY, SECRET_KEY_PREFIX + connectorId)
                    .build());
            break;
          case GCR:
            map.put(stepId,
                ConnectorConversionInfo.builder()
                    .connectorRef(connectorRef)
                    .envToSecretEntry(EnvVariableEnum.GCP_KEY_AS_FILE, SECRET_PATH_PREFIX + connectorId)
                    .build());
            break;
          case ARTIFACTORY:
            map.put(stepId,
                ConnectorConversionInfo.builder()
                    .connectorRef(connectorRef)
                    .envToSecretEntry(EnvVariableEnum.ARTIFACTORY_ENDPOINT, ENDPOINT_PREFIX + connectorId)
                    .envToSecretEntry(EnvVariableEnum.ARTIFACTORY_USERNAME, USERNAME_PREFIX + connectorId)
                    .envToSecretEntry(EnvVariableEnum.ARTIFACTORY_PASSWORD, PASSW_PREFIX + connectorId)
                    .build());
            break;
          case DOCKERHUB:
            map.put(stepId,
                ConnectorConversionInfo.builder()
                    .connectorRef(connectorRef)
                    .envToSecretEntry(EnvVariableEnum.DOCKER_REGISTRY, ENDPOINT_PREFIX + connectorId)
                    .envToSecretEntry(EnvVariableEnum.DOCKER_USERNAME, USERNAME_PREFIX + connectorId)
                    .envToSecretEntry(EnvVariableEnum.DOCKER_PASSWORD, PASSW_PREFIX + connectorId)
                    .build());
            break;
          default:
            throw new IllegalStateException("Unexpected value: " + stepElement.getType());
        }
      }
    } else if (stepElement.getStepSpecType() instanceof PluginCompatibleStep) {
      PluginCompatibleStep step = (PluginCompatibleStep) stepElement.getStepSpecType();
      switch (stepElement.getType()) {
        case "buildAndPushECR":
        case "restoreCacheS3":
        case "saveCacheS3":
        case "uploadToS3":
          map.put(step.getIdentifier(),
              ConnectorConversionInfo.builder()
                  .connectorRef(step.getConnectorRef())
                  .envToSecretEntry(EnvVariableEnum.AWS_ACCESS_KEY, PLUGIN_ACCESS_KEY)
                  .envToSecretEntry(EnvVariableEnum.AWS_SECRET_KEY, PLUGIN_SECRET_KEY)
                  .build());
          break;
        case "buildAndPushGCR":
        case "uploadToGCS":
        case "saveCacheGCS":
        case "restoreCacheGCS":
          map.put(step.getIdentifier(),
              ConnectorConversionInfo.builder()
                  .connectorRef(step.getConnectorRef())
                  .envToSecretEntry(EnvVariableEnum.GCP_KEY, PLUGIN_JSON_KEY)
                  .build());

          break;
        case "buildAndPushDockerHub":
          map.put(step.getIdentifier(),
              ConnectorConversionInfo.builder()
                  .connectorRef(step.getConnectorRef())
                  .envToSecretEntry(EnvVariableEnum.DOCKER_USERNAME, PLUGIN_USERNAME)
                  .envToSecretEntry(EnvVariableEnum.DOCKER_PASSWORD, PLUGIN_PASSW)
                  .envToSecretEntry(EnvVariableEnum.DOCKER_REGISTRY, PLUGIN_REGISTRY)
                  .build());
          break;
        default:
          throw new IllegalStateException("Unexpected value: " + stepElement.getType());
      }
    }
    return map;
  }

  private Map<String, String> getEnvVariables(IntegrationStage integrationStage) {
    if (isEmpty(integrationStage.getCustomVariables())) {
      return Collections.emptyMap();
    }

    return integrationStage.getCustomVariables()
        .stream()
        .filter(customVariables -> customVariables.getType().equals(CustomVariable.Type.TEXT))
        .map(customVariable -> (CustomTextVariable) customVariable)
        .collect(toMap(CustomTextVariable::getName, CustomTextVariable::getValue));
  }

  private ImageDetails getImageInfo(String image) {
    String tag = "";
    String name = image;

    if (image.contains(IMAGE_PATH_SPLIT_REGEX)) {
      String[] subTokens = image.split(IMAGE_PATH_SPLIT_REGEX);
      if (subTokens.length > 2) {
        throw new InvalidRequestException("Image should not contain multiple tags");
      }
      if (subTokens.length == 2) {
        name = subTokens[0];
        tag = subTokens[1];
      }
    }

    return ImageDetails.builder().name(name).tag(tag).build();
  }

  private Integer getContainerMemoryLimit(ContainerResource resource) {
    Integer memoryLimit = ciExecutionServiceConfig.getDefaultMemoryLimit();
    if (resource != null && resource.getLimit() != null && resource.getLimit().getMemory() != null) {
      memoryLimit = QuantityUtils.getMemoryQuantityValueInUnit(resource.getLimit().getMemory(), BinaryQuantityUnit.Mi);
    }
    return memoryLimit;
  }

  private Integer getContainerCpuLimit(ContainerResource resource) {
    Integer cpuLimit = ciExecutionServiceConfig.getDefaultCPULimit();
    if (resource != null && resource.getLimit() != null && resource.getLimit().getCpu() != null) {
      cpuLimit = QuantityUtils.getCpuQuantityValueInUnit(resource.getLimit().getCpu(), DecimalQuantityUnit.m);
    }
    return cpuLimit;
  }

  private Integer getStageMemoryRequest(List<ExecutionWrapper> steps) {
    Integer stageMemoryRequest = 0;
    for (ExecutionWrapper step : steps) {
      Integer executionWrapperMemoryRequest = getExecutionWrapperMemoryRequest(step);
      stageMemoryRequest = Math.max(stageMemoryRequest, executionWrapperMemoryRequest);
    }
    return stageMemoryRequest;
  }

  private Integer getExecutionWrapperMemoryRequest(ExecutionWrapper executionWrapper) {
    if (executionWrapper == null) {
      return 0;
    }

    Integer executionWrapperMemoryRequest = 0;
    if (executionWrapper instanceof StepElement) {
      executionWrapperMemoryRequest = getStepMemoryLimit((StepElement) executionWrapper);
    } else if (executionWrapper instanceof ParallelStepElement) {
      ParallelStepElement parallel = (ParallelStepElement) executionWrapper;
      if (parallel.getSections() != null) {
        for (ExecutionWrapper wrapper : parallel.getSections()) {
          executionWrapperMemoryRequest += getExecutionWrapperMemoryRequest(wrapper);
        }
      }
    }
    return executionWrapperMemoryRequest;
  }

  private Integer getStepMemoryLimit(StepElement stepElement) {
    Integer zeroMemory = 0;
    if (!(stepElement.getStepSpecType() instanceof CIStepInfo)) {
      return zeroMemory;
    }

    CIStepInfo ciStepInfo = (CIStepInfo) stepElement.getStepSpecType();
    switch (ciStepInfo.getNonYamlInfo().getStepInfoType()) {
      case RUN:
        return getContainerMemoryLimit(((RunStepInfo) ciStepInfo).getResources());
      case PLUGIN:
        return getContainerMemoryLimit(((PluginStepInfo) ciStepInfo).getResources());
      case TEST_INTELLIGENCE:
        return getContainerMemoryLimit(((TestIntelligenceStepInfo) ciStepInfo).getResources());
      case GCR:
      case ECR:
      case DOCKER:
      case UPLOAD_GCS:
      case UPLOAD_S3:
      case RESTORE_CACHE_GCS:
      case RESTORE_CACHE_S3:
      case SAVE_CACHE_S3:
      case SAVE_CACHE_GCS:
        return getContainerMemoryLimit(((PluginCompatibleStep) ciStepInfo).getResources());
      default:
        return zeroMemory;
    }
  }

  private Integer getStageCpuRequest(List<ExecutionWrapper> steps) {
    Integer stageCpuRequest = 0;
    for (ExecutionWrapper step : steps) {
      Integer executionWrapperCpuRequest = getExecutionWrapperCpuRequest(step);
      stageCpuRequest = Math.max(stageCpuRequest, executionWrapperCpuRequest);
    }
    return stageCpuRequest;
  }

  private Integer getExecutionWrapperCpuRequest(ExecutionWrapper executionWrapper) {
    if (executionWrapper == null) {
      return 0;
    }

    Integer executionWrapperCpuRequest = 0;
    if (executionWrapper instanceof StepElement) {
      executionWrapperCpuRequest = getStepCpuLimit((StepElement) executionWrapper);
    } else if (executionWrapper instanceof ParallelStepElement) {
      ParallelStepElement parallel = (ParallelStepElement) executionWrapper;
      if (parallel.getSections() != null) {
        for (ExecutionWrapper wrapper : parallel.getSections()) {
          executionWrapperCpuRequest += getExecutionWrapperCpuRequest(wrapper);
        }
      }
    }
    return executionWrapperCpuRequest;
  }

  private Integer getStepCpuLimit(StepElement stepElement) {
    Integer zeroCpu = 0;
    if (!(stepElement.getStepSpecType() instanceof CIStepInfo)) {
      return zeroCpu;
    }

    CIStepInfo ciStepInfo = (CIStepInfo) stepElement.getStepSpecType();
    switch (ciStepInfo.getNonYamlInfo().getStepInfoType()) {
      case RUN:
        return getContainerCpuLimit(((RunStepInfo) ciStepInfo).getResources());
      case PLUGIN:
        return getContainerCpuLimit(((PluginStepInfo) ciStepInfo).getResources());
      case TEST_INTELLIGENCE:
        return getContainerCpuLimit(((TestIntelligenceStepInfo) ciStepInfo).getResources());
      case GCR:
      case ECR:
      case DOCKER:
      case UPLOAD_GCS:
      case UPLOAD_S3:
      case RESTORE_CACHE_GCS:
      case RESTORE_CACHE_S3:
      case SAVE_CACHE_S3:
      case SAVE_CACHE_GCS:
        return getContainerCpuLimit(((PluginCompatibleStep) ciStepInfo).getResources());
      default:
        return zeroCpu;
    }
  }

  private String getWorkingDirectory(IntegrationStage integrationStage) {
    if (isNotEmpty(integrationStage.getWorkingDirectory())) {
      return integrationStage.getWorkingDirectory();
    } else {
      return CICommonPodConstants.STEP_EXEC_WORKING_DIR;
    }
  }

  private String getWorkingDirectoryPath(String workingDirectory) {
    return String.format("/%s/%s", STEP_EXEC, workingDirectory);
  }
}
