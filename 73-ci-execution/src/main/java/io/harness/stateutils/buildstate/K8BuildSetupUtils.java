package io.harness.stateutils.buildstate;

import static io.harness.common.CIExecutionConstants.ACCESS_KEY_MINIO_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_ACCOUNT_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_BUILD_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_ORG_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_PROJECT_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_STAGE_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HOME_VARIABLE;
import static io.harness.common.CIExecutionConstants.LOG_SERVICE_ENDPOINT_VARIABLE;
import static io.harness.common.CIExecutionConstants.LOG_SERVICE_ENDPOINT_VARIABLE_VALUE;
import static io.harness.common.CIExecutionConstants.SECRET_KEY_MINIO_VARIABLE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static software.wings.common.CICommonPodConstants.MOUNT_PATH;
import static software.wings.common.CICommonPodConstants.STEP_EXEC;
import static software.wings.common.CICommonPodConstants.STEP_EXEC_WORKING_DIR;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ambiance.Ambiance;
import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.environment.pod.PodSetupInfo;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.serializer.ExecutionProtobufSerializer;
import io.harness.beans.steps.stepinfo.BuildEnvSetupStepInfo;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.exception.InvalidRequestException;
import io.harness.managerclient.ManagerCIResource;
import io.harness.network.SafeHttpCall;
import io.harness.product.ci.engine.proto.Execution;
import io.harness.references.SweepingOutputRefObject;
import io.harness.rest.RestResponse;
import io.harness.security.encryption.EncryptableSettingWithEncryptionDetails;
import io.harness.stateutils.buildstate.providers.InternalContainerParamsProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8ContainerParams.CIK8ContainerParamsBuilder;
import software.wings.beans.ci.pod.CIK8PodParams;
import software.wings.beans.ci.pod.ContainerSecrets;
import software.wings.beans.ci.pod.EncryptedVariableWithType;
import software.wings.beans.ci.pod.ImageDetailsWithConnector;
import software.wings.beans.ci.pod.PVCParams;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Singleton
@Slf4j
public class K8BuildSetupUtils {
  @Inject private ManagerCIResource managerCIResource;
  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject ExecutionProtobufSerializer protobufSerializer;
  @Inject ServiceTokenUtils serviceTokenUtils;

  public RestResponse<K8sTaskExecutionResponse> executeCISetupTask(
      BuildEnvSetupStepInfo buildEnvSetupStepInfo, Ambiance ambiance) {
    try {
      K8PodDetails k8PodDetails = (K8PodDetails) executionSweepingOutputResolver.resolve(
          ambiance, SweepingOutputRefObject.builder().name(ContextElement.podDetails).build());

      final String clusterName = k8PodDetails.getClusterName();
      PodSetupInfo podSetupInfo = getPodSetupInfo((K8BuildJobEnvInfo) buildEnvSetupStepInfo.getBuildJobEnvInfo());

      Set<String> publishStepConnectorIdentifier =
          ((K8BuildJobEnvInfo) buildEnvSetupStepInfo.getBuildJobEnvInfo()).getPublishStepConnectorIdentifier();

      // TODO Use k8 connector from element input
      logger.info("Sending pod creation task for {}", podSetupInfo.getName());
      return SafeHttpCall.execute(managerCIResource.createK8PodTask(k8PodDetails.getAccountId(), clusterName,
          buildEnvSetupStepInfo.getGitConnectorIdentifier(), buildEnvSetupStepInfo.getBranchName(),
          getPodParams(podSetupInfo, k8PodDetails, null, publishStepConnectorIdentifier, false)));

    } catch (Exception e) {
      logger.error("build state execution failed", e);
    }
    return null;
  }

  public RestResponse<K8sTaskExecutionResponse> executeK8sCILiteEngineTask(
      LiteEngineTaskStepInfo liteEngineTaskStepInfo, Ambiance ambiance) {
    K8PodDetails k8PodDetails = (K8PodDetails) executionSweepingOutputResolver.resolve(
        ambiance, SweepingOutputRefObject.builder().name(ContextElement.podDetails).build());

    final String clusterName = k8PodDetails.getClusterName();

    try {
      PodSetupInfo podSetupInfo = getPodSetupInfo((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo());

      Set<String> publishStepConnectorIdentifier =
          ((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo()).getPublishStepConnectorIdentifier();

      // TODO Use k8 connector from element input
      return SafeHttpCall.execute(managerCIResource.createK8PodTask(k8PodDetails.getAccountId(), clusterName,
          liteEngineTaskStepInfo.getGitConnectorIdentifier(), liteEngineTaskStepInfo.getBranchName(),
          getPodParams(podSetupInfo, k8PodDetails, liteEngineTaskStepInfo, publishStepConnectorIdentifier,
              liteEngineTaskStepInfo.isUsePVC())));
    } catch (Exception e) {
      logger.error("lite engine task state execution failed", e);
    }
    return null;
  }

  public CIK8PodParams<CIK8ContainerParams> getPodParams(PodSetupInfo podSetupInfo, K8PodDetails k8PodDetails,
      LiteEngineTaskStepInfo liteEngineTaskStepInfo, Set<String> publishStepConnectorIdentifier, boolean usePVC) {
    final String namespace = k8PodDetails.getNamespace();
    Map<String, String> map = new HashMap<>();
    map.put(STEP_EXEC, MOUNT_PATH);

    // user input container with custom entry point
    List<CIK8ContainerParams> containerParams =
        podSetupInfo.getPodSetupParams()
            .getContainerDefinitionInfos()
            .stream()
            .map(containerDefinitionInfo -> createCIK8ContainerParams(containerDefinitionInfo, map, k8PodDetails))
            .collect(toList());

    // include lite-engine container
    CIK8ContainerParams liteEngineContainerParams =
        createLiteEngineContainerParams(liteEngineTaskStepInfo, publishStepConnectorIdentifier, k8PodDetails,
            podSetupInfo.getStageCpuRequest(), podSetupInfo.getStageMemoryRequest());
    containerParams.add(liteEngineContainerParams);

    CIK8ContainerParams setupAddOnContainerParams =
        InternalContainerParamsProvider.getSetupAddonContainerParams().build();

    List<PVCParams> pvcParams = new ArrayList<>();
    if (usePVC) {
      pvcParams = Collections.singletonList(podSetupInfo.getPvcParams());
    }
    return CIK8PodParams.<CIK8ContainerParams>builder()
        .name(podSetupInfo.getName())
        .namespace(namespace)
        .stepExecVolumeName(STEP_EXEC)
        .stepExecWorkingDir(STEP_EXEC_WORKING_DIR)
        .containerParamsList(containerParams)
        .pvcParamList(pvcParams)
        .initContainerParamsList(Collections.singletonList(setupAddOnContainerParams))
        .build();
  }

  private CIK8ContainerParams createCIK8ContainerParams(ContainerDefinitionInfo containerDefinitionInfo,
      Map<String, String> volumeToMountPath, K8PodDetails k8PodDetails) {
    Map<String, String> envVars = getCommonStepEnvVariables(k8PodDetails);
    if (isNotEmpty(containerDefinitionInfo.getEnvVars())) {
      envVars.putAll(containerDefinitionInfo.getEnvVars()); // Put customer input env variables
    }
    return CIK8ContainerParams.builder()
        .name(containerDefinitionInfo.getName())
        .containerResourceParams(containerDefinitionInfo.getContainerResourceParams())
        .containerType(containerDefinitionInfo.getContainerType())
        .envVars(envVars)
        .containerSecrets(
            ContainerSecrets.builder().encryptedSecrets(getSecretEnvVars(containerDefinitionInfo)).build())
        .commands(containerDefinitionInfo.getCommands())
        .ports(containerDefinitionInfo.getPorts())
        .args(containerDefinitionInfo.getArgs())
        .imageDetailsWithConnector(
            ImageDetailsWithConnector.builder()
                .imageDetails(containerDefinitionInfo.getContainerImageDetails().getImageDetails())
                .connectorName(containerDefinitionInfo.getContainerImageDetails().getConnectorIdentifier())
                .build())
        .volumeToMountPath(volumeToMountPath)
        .workingDir(getWorkingDirectoryPath())
        .build();
  }

  private CIK8ContainerParams createLiteEngineContainerParams(LiteEngineTaskStepInfo liteEngineTaskStepInfo,
      Set<String> publishStepConnectorIdentifier, K8PodDetails k8PodDetails, Integer stageCpuRequest,
      Integer stageMemoryRequest) {
    String serializedLiteEngineStepInfo = getSerializedLiteEngineStepInfo(liteEngineTaskStepInfo);
    String serviceToken = serviceTokenUtils.getServiceToken();
    CIK8ContainerParamsBuilder liteEngineContainerParamsBuilder =
        InternalContainerParamsProvider.getLiteEngineContainerParams(
            k8PodDetails, serializedLiteEngineStepInfo, serviceToken, stageCpuRequest, stageMemoryRequest);

    liteEngineContainerParamsBuilder.containerSecrets(
        ContainerSecrets.builder()
            .publishArtifactEncryptedValues(getPublishArtifactEncryptedValues(publishStepConnectorIdentifier))
            .build());
    return liteEngineContainerParamsBuilder.build();
  }

  private String getSerializedLiteEngineStepInfo(LiteEngineTaskStepInfo liteEngineTaskStepInfo) {
    Execution executionPrototype = protobufSerializer.convertExecutionElement(liteEngineTaskStepInfo.getSteps());
    Execution execution =
        Execution.newBuilder(executionPrototype).setAccountId(liteEngineTaskStepInfo.getAccountId()).build();
    return Base64.encodeBase64String(execution.toByteArray());
  }

  private Map<String, EncryptableSettingWithEncryptionDetails> getPublishArtifactEncryptedValues(
      Set<String> publishStepConnectorIdentifier) {
    Map<String, EncryptableSettingWithEncryptionDetails> publishArtifactEncryptedValues = new HashMap<>();

    if (isNotEmpty(publishStepConnectorIdentifier)) {
      // TODO Harsh Fetch connector encrypted values once connector APIs will be ready
      for (String connectorIdentifier : publishStepConnectorIdentifier) {
        publishArtifactEncryptedValues.put(connectorIdentifier, null);
      }
    }
    return publishArtifactEncryptedValues;
  }

  @NotNull
  private PodSetupInfo getPodSetupInfo(K8BuildJobEnvInfo k8BuildJobEnvInfo) {
    // Supporting single pod currently
    Optional<PodSetupInfo> podSetupInfoOpt =
        k8BuildJobEnvInfo.getPodsSetupInfo().getPodSetupInfoList().stream().findFirst();
    if (!podSetupInfoOpt.isPresent()) {
      throw new InvalidRequestException("Pod setup info can not be empty");
    }
    return podSetupInfoOpt.get();
  }

  @NotNull
  private Map<String, EncryptedVariableWithType> getSecretEnvVars(ContainerDefinitionInfo containerDefinitionInfo) {
    Map<String, EncryptedVariableWithType> envSecretVars = new HashMap<>();
    if (isNotEmpty(containerDefinitionInfo.getEncryptedSecrets())) {
      envSecretVars.putAll(containerDefinitionInfo.getEncryptedSecrets()); // Put customer input env variables
    }
    // Put Harness internal env variable like that of minio
    // TODO Replace null with encrypted values once cdng secret apis are ready
    envSecretVars.put(ACCESS_KEY_MINIO_VARIABLE, null);
    envSecretVars.put(SECRET_KEY_MINIO_VARIABLE, null);

    return envSecretVars;
  }

  @NotNull
  private Map<String, String> getCommonStepEnvVariables(K8PodDetails k8PodDetails) {
    Map<String, String> envVars = new HashMap<>();
    final String accountID = k8PodDetails.getBuildNumber().getAccountIdentifier();
    final String projectID = k8PodDetails.getBuildNumber().getProjectIdentifier();
    final String orgID = k8PodDetails.getBuildNumber().getOrgIdentifier();
    final Long buildNumber = k8PodDetails.getBuildNumber().getBuildNumber();
    final String stageID = k8PodDetails.getStageID();

    // Add environment variables that need to be used to stream logs
    envVars.put(HOME_VARIABLE, getWorkingDirectoryPath());
    envVars.put(LOG_SERVICE_ENDPOINT_VARIABLE, LOG_SERVICE_ENDPOINT_VARIABLE_VALUE);
    envVars.put(HARNESS_ACCOUNT_ID_VARIABLE, accountID);
    envVars.put(HARNESS_PROJECT_ID_VARIABLE, projectID);
    envVars.put(HARNESS_ORG_ID_VARIABLE, orgID);
    envVars.put(HARNESS_BUILD_ID_VARIABLE, buildNumber.toString());
    envVars.put(HARNESS_STAGE_ID_VARIABLE, stageID);
    return envVars;
  }

  private String getWorkingDirectoryPath() {
    return String.format("/%s/%s", STEP_EXEC, STEP_EXEC_WORKING_DIR);
  }
}
