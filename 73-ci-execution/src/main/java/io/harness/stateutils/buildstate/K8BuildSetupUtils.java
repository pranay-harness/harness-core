package io.harness.stateutils.buildstate;

import static io.harness.common.CIExecutionConstants.ACCESS_KEY_MINIO_VARIABLE;
import static io.harness.common.CIExecutionConstants.DEFAULT_INTERNAL_IMAGE_CONNECTOR;
import static io.harness.common.CIExecutionConstants.HARNESS_ACCOUNT_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_BUILD_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_ORG_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_PROJECT_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.HARNESS_STAGE_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.LOCALHOST_IP;
import static io.harness.common.CIExecutionConstants.LOG_SERVICE_ENDPOINT_VARIABLE;
import static io.harness.common.CIExecutionConstants.LOG_SERVICE_TOKEN_VARIABLE;
import static io.harness.common.CIExecutionConstants.SECRET_KEY_MINIO_VARIABLE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static software.wings.common.CICommonPodConstants.STEP_EXEC;
import static software.wings.common.CICommonPodConstants.STEP_EXEC_WORKING_DIR;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ambiance.Ambiance;
import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.environment.pod.PodSetupInfo;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.serializer.ExecutionProtobufSerializer;
import io.harness.beans.steps.stepinfo.BuildEnvSetupStepInfo;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.beans.yaml.extended.CustomSecretVariable;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logserviceclient.CILogServiceUtils;
import io.harness.ng.core.NGAccess;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.product.ci.engine.proto.Execution;
import io.harness.references.SweepingOutputRefObject;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.stateutils.buildstate.providers.InternalContainerParamsProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.TaskType;
import software.wings.beans.ci.CIK8BuildTaskParams;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8PodParams;
import software.wings.beans.ci.pod.ConnectorDetails;
import software.wings.beans.ci.pod.ContainerSecrets;
import software.wings.beans.ci.pod.HostAliasParams;
import software.wings.beans.ci.pod.ImageDetailsWithConnector;
import software.wings.beans.ci.pod.ImageDetailsWithConnector.ImageDetailsWithConnectorBuilder;
import software.wings.beans.ci.pod.PVCParams;
import software.wings.beans.ci.pod.SecretVariableDetails;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Singleton
@Slf4j
public class K8BuildSetupUtils {
  @Inject private SecretVariableUtils secretVariableUtils;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject private ServiceTokenUtils serviceTokenUtils;
  @Inject private ConnectorUtils connectorUtils;
  @Inject private InternalContainerParamsProvider internalContainerParamsProvider;
  @Inject private ExecutionProtobufSerializer protobufSerializer;
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @Inject CILogServiceUtils logServiceUtils;

  public K8sTaskExecutionResponse executeCISetupTask(BuildEnvSetupStepInfo buildEnvSetupStepInfo, Ambiance ambiance) {
    try {
      K8PodDetails k8PodDetails = (K8PodDetails) executionSweepingOutputResolver.resolve(
          ambiance, SweepingOutputRefObject.builder().name(ContextElement.podDetails).build());

      final String clusterName = k8PodDetails.getClusterName();
      PodSetupInfo podSetupInfo = getPodSetupInfo((K8BuildJobEnvInfo) buildEnvSetupStepInfo.getBuildJobEnvInfo());

      Set<String> publishStepConnectorIdentifier =
          ((K8BuildJobEnvInfo) buildEnvSetupStepInfo.getBuildJobEnvInfo()).getPublishStepConnectorIdentifier();

      NGAccess ngAccess = AmbianceHelper.getNgAccess(ambiance);

      CIK8BuildTaskParams cik8BuildTaskParams =
          CIK8BuildTaskParams.builder()
              .k8sConnector(connectorUtils.getConnectorDetails(ngAccess, clusterName))
              .cik8PodParams(
                  getPodParams(ngAccess, podSetupInfo, k8PodDetails, null, publishStepConnectorIdentifier, false))
              .build();

      DelegateTaskRequest delegateTaskRequest =
          DelegateTaskRequest.builder()
              .accountId(ngAccess.getAccountIdentifier())
              .taskSetupAbstractions(ambiance.getSetupAbstractions())
              .executionTimeout(Duration.ofSeconds(buildEnvSetupStepInfo.getTimeout()))
              .taskType(TaskType.CI_BUILD.name())
              .taskParameters(cik8BuildTaskParams)
              .taskDescription("CI build task BuildEnvSetupStepInfo")
              .build();

      log.info("Sending pod creation task for {}", podSetupInfo.getName());
      K8sTaskExecutionResponse k8sTaskExecutionResponse =
          (K8sTaskExecutionResponse) delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
      if (k8sTaskExecutionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
        log.info("Pod creation task for {} executed successfully", podSetupInfo.getName());
        return k8sTaskExecutionResponse;
      } else {
        log.error("build env setup task state execution finished with status {}",
            k8sTaskExecutionResponse.getCommandExecutionStatus());
      }
    } catch (Exception e) {
      log.error("build env setup state execution failed", e);
    }
    return K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
  }

  public K8sTaskExecutionResponse executeK8sCILiteEngineTask(
      LiteEngineTaskStepInfo liteEngineTaskStepInfo, Ambiance ambiance) {
    K8PodDetails k8PodDetails = (K8PodDetails) executionSweepingOutputResolver.resolve(
        ambiance, SweepingOutputRefObject.builder().name(ContextElement.podDetails).build());

    final String clusterName = k8PodDetails.getClusterName();

    try {
      PodSetupInfo podSetupInfo = getPodSetupInfo((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo());

      Set<String> publishStepConnectorIdentifier =
          ((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo()).getPublishStepConnectorIdentifier();

      NGAccess ngAccess = AmbianceHelper.getNgAccess(ambiance);

      CIK8BuildTaskParams cik8BuildTaskParams =
          CIK8BuildTaskParams.builder()
              .k8sConnector(connectorUtils.getConnectorDetails(ngAccess, clusterName))
              .cik8PodParams(getPodParams(ngAccess, podSetupInfo, k8PodDetails, liteEngineTaskStepInfo,
                  publishStepConnectorIdentifier, liteEngineTaskStepInfo.isUsePVC()))
              .build();

      DelegateTaskRequest delegateTaskRequest =
          DelegateTaskRequest.builder()
              .accountId(ngAccess.getAccountIdentifier())
              .taskSetupAbstractions(ambiance.getSetupAbstractions())
              .executionTimeout(Duration.ofSeconds(liteEngineTaskStepInfo.getTimeout()))
              .taskType(TaskType.CI_BUILD.name())
              .taskParameters(cik8BuildTaskParams)
              .taskDescription("CI build task LiteEngineTaskStepInfo")
              .build();

      log.info("Sending pod creation task for {}", podSetupInfo.getName());
      K8sTaskExecutionResponse k8sTaskExecutionResponse =
          (K8sTaskExecutionResponse) delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
      if (k8sTaskExecutionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS) {
        log.info("Pod creation task for {} executed successfully", podSetupInfo.getName());
      } else {
        log.error("lite engine task state execution finished with status {}",
            k8sTaskExecutionResponse.getCommandExecutionStatus());
      }
      return k8sTaskExecutionResponse;
    } catch (Exception e) {
      log.error("lite engine task state execution failed", e);
    }
    return K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
  }

  public CIK8PodParams<CIK8ContainerParams> getPodParams(NGAccess ngAccess, PodSetupInfo podSetupInfo,
      K8PodDetails k8PodDetails, LiteEngineTaskStepInfo liteEngineTaskStepInfo,
      Set<String> publishStepConnectorIdentifier, boolean usePVC) throws Exception {
    ConnectorDetails harnessInternalImageRegistryConnectorDetails =
        connectorUtils.getConnectorDetails(ngAccess, DEFAULT_INTERNAL_IMAGE_CONNECTOR);

    List<CIK8ContainerParams> containerParamsList = getContainerParamsList(k8PodDetails, podSetupInfo, ngAccess,
        publishStepConnectorIdentifier, harnessInternalImageRegistryConnectorDetails, liteEngineTaskStepInfo);

    CIK8ContainerParams setupAddOnContainerParams =
        internalContainerParamsProvider.getSetupAddonContainerParams(harnessInternalImageRegistryConnectorDetails);

    List<HostAliasParams> hostAliasParamsList = new ArrayList<>();
    if (podSetupInfo.getServiceIdList() != null) {
      hostAliasParamsList.add(
          HostAliasParams.builder().ipAddress(LOCALHOST_IP).hostnameList(podSetupInfo.getServiceIdList()).build());
    }

    List<PVCParams> pvcParams = new ArrayList<>();
    if (usePVC) {
      pvcParams = singletonList(podSetupInfo.getPvcParams());
    }
    return CIK8PodParams.<CIK8ContainerParams>builder()
        .name(podSetupInfo.getName())
        .namespace(k8PodDetails.getNamespace())
        .gitConnector(connectorUtils.getConnectorDetails(ngAccess, liteEngineTaskStepInfo.getGitConnectorIdentifier()))
        .branchName(liteEngineTaskStepInfo.getBranchName())
        .stepExecVolumeName(STEP_EXEC)
        .stepExecWorkingDir(STEP_EXEC_WORKING_DIR)
        .containerParamsList(containerParamsList)
        .pvcParamList(pvcParams)
        .initContainerParamsList(singletonList(setupAddOnContainerParams))
        .hostAliasParamsList(hostAliasParamsList)
        .build();
  }

  public List<CIK8ContainerParams> getContainerParamsList(K8PodDetails k8PodDetails, PodSetupInfo podSetupInfo,
      NGAccess ngAccess, Set<String> publishStepConnectorIdentifier,
      ConnectorDetails harnessInternalImageRegistryConnectorDetails, LiteEngineTaskStepInfo liteEngineTaskStepInfo)
      throws Exception {
    Map<String, String> logEnvVars = getLogServiceEnvVariables(k8PodDetails);

    // user input containers with custom entry point
    List<CIK8ContainerParams> containerParams =
        podSetupInfo.getPodSetupParams()
            .getContainerDefinitionInfos()
            .stream()
            .map(containerDefinitionInfo
                -> createCIK8ContainerParams(ngAccess, containerDefinitionInfo, k8PodDetails, logEnvVars))
            .collect(toList());

    // include lite-engine container
    Map<String, ConnectorDetails> publishArtifactConnectorDetailsMap =
        connectorUtils.getConnectorDetailsMap(ngAccess, publishStepConnectorIdentifier);
    CIK8ContainerParams liteEngineContainerParams =
        createLiteEngineContainerParams(harnessInternalImageRegistryConnectorDetails,
            publishArtifactConnectorDetailsMap, liteEngineTaskStepInfo, k8PodDetails, podSetupInfo.getStageCpuRequest(),
            podSetupInfo.getStageMemoryRequest(), podSetupInfo.getServiceGrpcPortList(), logEnvVars);
    containerParams.add(liteEngineContainerParams);

    return containerParams;
  }

  private CIK8ContainerParams createCIK8ContainerParams(NGAccess ngAccess,
      ContainerDefinitionInfo containerDefinitionInfo, K8PodDetails k8PodDetails, Map<String, String> logEnvVars) {
    Map<String, String> envVars = getCommonStepEnvVariables(k8PodDetails, logEnvVars);
    if (isNotEmpty(containerDefinitionInfo.getEnvVars())) {
      envVars.putAll(containerDefinitionInfo.getEnvVars()); // Put customer input env variables
    }

    ImageDetailsWithConnectorBuilder imageDetailsWithConnectorBuilder =
        ImageDetailsWithConnector.builder().imageDetails(
            containerDefinitionInfo.getContainerImageDetails().getImageDetails());
    if (containerDefinitionInfo.getContainerImageDetails().getConnectorIdentifier() != null) {
      imageDetailsWithConnectorBuilder.imageConnectorDetails(connectorUtils.getConnectorDetails(
          ngAccess, containerDefinitionInfo.getContainerImageDetails().getConnectorIdentifier()));
    }

    return CIK8ContainerParams.builder()
        .name(containerDefinitionInfo.getName())
        .containerResourceParams(containerDefinitionInfo.getContainerResourceParams())
        .containerType(containerDefinitionInfo.getContainerType())
        .envVars(envVars)
        .containerSecrets(ContainerSecrets.builder()
                              .secretVariableDetails(getSecretVariableDetails(ngAccess, containerDefinitionInfo))
                              .build())
        .commands(containerDefinitionInfo.getCommands())
        .ports(containerDefinitionInfo.getPorts())
        .args(containerDefinitionInfo.getArgs())
        .imageDetailsWithConnector(imageDetailsWithConnectorBuilder.build())
        .volumeToMountPath(containerDefinitionInfo.getVolumeToMountPath())
        .workingDir(containerDefinitionInfo.getWorkingDirectory())
        .build();
  }

  private CIK8ContainerParams createLiteEngineContainerParams(ConnectorDetails connectorDetails,
      Map<String, ConnectorDetails> publishArtifactConnectors, LiteEngineTaskStepInfo liteEngineTaskStepInfo,
      K8PodDetails k8PodDetails, Integer stageCpuRequest, Integer stageMemoryRequest, List<Integer> serviceGrpcPortList,
      Map<String, String> logEnvVars) {
    String serializedLiteEngineStepInfo = getSerializedLiteEngineStepInfo(liteEngineTaskStepInfo);
    String serviceToken = serviceTokenUtils.getServiceToken();
    return internalContainerParamsProvider.getLiteEngineContainerParams(connectorDetails, publishArtifactConnectors,
        k8PodDetails, serializedLiteEngineStepInfo, serviceToken, stageCpuRequest, stageMemoryRequest,
        serviceGrpcPortList, logEnvVars);
  }

  private String getSerializedLiteEngineStepInfo(LiteEngineTaskStepInfo liteEngineTaskStepInfo) {
    Execution executionPrototype = protobufSerializer.convertExecutionElement(liteEngineTaskStepInfo.getSteps());
    Execution execution =
        Execution.newBuilder(executionPrototype).setAccountId(liteEngineTaskStepInfo.getAccountId()).build();
    return Base64.encodeBase64String(execution.toByteArray());
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
  private List<SecretVariableDetails> getSecretVariableDetails(
      NGAccess ngAccess, ContainerDefinitionInfo containerDefinitionInfo) {
    List<SecretVariableDetails> secretVariableDetails = new ArrayList<>();
    if (isNotEmpty(containerDefinitionInfo.getSecretVariables())) {
      containerDefinitionInfo.getSecretVariables().forEach(secretVariable
          -> secretVariableDetails.add(secretVariableUtils.getSecretVariableDetails(ngAccess, secretVariable)));
    }

    secretVariableDetails.add(secretVariableUtils.getSecretVariableDetails(ngAccess,
        CustomSecretVariable.builder()
            .name(ACCESS_KEY_MINIO_VARIABLE)
            .value(SecretRefData.builder().scope(Scope.ACCOUNT).identifier(ACCESS_KEY_MINIO_VARIABLE).build())
            .build()));
    secretVariableDetails.add(secretVariableUtils.getSecretVariableDetails(ngAccess,
        CustomSecretVariable.builder()
            .name(SECRET_KEY_MINIO_VARIABLE)
            .value(SecretRefData.builder().scope(Scope.ACCOUNT).identifier(SECRET_KEY_MINIO_VARIABLE).build())
            .build()));
    return secretVariableDetails;
  }

  @NotNull
  private Map<String, String> getLogServiceEnvVariables(K8PodDetails k8PodDetails) throws Exception {
    Map<String, String> envVars = new HashMap<>();
    final String accountID = k8PodDetails.getBuildNumber().getAccountIdentifier();
    final String logServiceBaseUrl = logServiceUtils.getLogServiceConfig().getBaseUrl();

    // Make a call to the log service and get back the token
    String logServiceToken = logServiceUtils.getLogServiceToken(accountID);
    envVars.put(LOG_SERVICE_TOKEN_VARIABLE, logServiceToken);
    envVars.put(LOG_SERVICE_ENDPOINT_VARIABLE, logServiceBaseUrl);

    return envVars;
  }

  @NotNull
  private Map<String, String> getCommonStepEnvVariables(K8PodDetails k8PodDetails, Map<String, String> logEnvVars) {
    Map<String, String> envVars = new HashMap<>();
    final String accountID = k8PodDetails.getBuildNumber().getAccountIdentifier();
    final String projectID = k8PodDetails.getBuildNumber().getProjectIdentifier();
    final String orgID = k8PodDetails.getBuildNumber().getOrgIdentifier();
    final Long buildNumber = k8PodDetails.getBuildNumber().getBuildNumber();
    final String stageID = k8PodDetails.getStageID();

    // Add log service environment variables
    envVars.putAll(logEnvVars);

    // Add other environment variables needed in the containers
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
