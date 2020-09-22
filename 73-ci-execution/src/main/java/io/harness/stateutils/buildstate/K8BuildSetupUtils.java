package io.harness.stateutils.buildstate;

import static io.harness.common.CIExecutionConstants.ACCESS_KEY_MINIO_VARIABLE;
import static io.harness.common.CIExecutionConstants.BUCKET_MINIO_VARIABLE;
import static io.harness.common.CIExecutionConstants.BUCKET_MINIO_VARIABLE_VALUE;
import static io.harness.common.CIExecutionConstants.DELEGATE_SERVICE_ENDPOINT_VARIABLE;
import static io.harness.common.CIExecutionConstants.DELEGATE_SERVICE_ENDPOINT_VARIABLE_VALUE;
import static io.harness.common.CIExecutionConstants.DELEGATE_SERVICE_ID_VARIABLE;
import static io.harness.common.CIExecutionConstants.DELEGATE_SERVICE_ID_VARIABLE_VALUE;
import static io.harness.common.CIExecutionConstants.DELEGATE_SERVICE_TOKEN_VARIABLE;
import static io.harness.common.CIExecutionConstants.ENDPOINT_MINIO_VARIABLE;
import static io.harness.common.CIExecutionConstants.ENDPOINT_MINIO_VARIABLE_VALUE;
import static io.harness.common.CIExecutionConstants.SECRET_KEY_MINIO_VARIABLE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.stateutils.buildstate.providers.InternalContainerParamsProvider.ContainerKind.ADDON_CONTAINER;
import static io.harness.stateutils.buildstate.providers.InternalContainerParamsProvider.ContainerKind.LITE_ENGINE_CONTAINER;
import static java.util.Arrays.asList;
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
import io.harness.data.structure.EmptyPredicate;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.exception.InvalidArgumentsException;
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
import software.wings.beans.ci.pod.CIContainerType;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8ContainerParams.CIK8ContainerParamsBuilder;
import software.wings.beans.ci.pod.CIK8PodParams;
import software.wings.beans.ci.pod.ContainerSecrets;
import software.wings.beans.ci.pod.EncryptedVariableWithType;
import software.wings.beans.ci.pod.ImageDetailsWithConnector;
import software.wings.beans.ci.pod.PVCParams;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class K8BuildSetupUtils {
  @Inject private ManagerCIResource managerCIResource;
  @Inject private LiteEngineTaskUtils liteEngineTaskUtils;
  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Inject ServiceTokenUtils serviceTokenUtils;
  @Inject ExecutionProtobufSerializer protobufSerializer;

  public RestResponse<K8sTaskExecutionResponse> executeCISetupTask(
      BuildEnvSetupStepInfo buildEnvSetupStepInfo, Ambiance ambiance) {
    try {
      K8PodDetails k8PodDetails = (K8PodDetails) executionSweepingOutputResolver.resolve(
          ambiance, SweepingOutputRefObject.builder().name(ContextElement.podDetails).build());

      final String namespace = k8PodDetails.getNamespace();
      final String clusterName = k8PodDetails.getClusterName();
      PodSetupInfo podSetupInfo = getPodSetupInfo((K8BuildJobEnvInfo) buildEnvSetupStepInfo.getBuildJobEnvInfo());

      Set<String> publishStepConnectorIdentifier =
          ((K8BuildJobEnvInfo) buildEnvSetupStepInfo.getBuildJobEnvInfo()).getPublishStepConnectorIdentifier();

      // TODO Use k8 connector from element input
      logger.info("Sending pod creation task for {}", podSetupInfo.getName());
      return SafeHttpCall.execute(managerCIResource.createK8PodTask(clusterName,
          buildEnvSetupStepInfo.getGitConnectorIdentifier(), buildEnvSetupStepInfo.getBranchName(),
          getPodParams(podSetupInfo, namespace, null, publishStepConnectorIdentifier, false)));

    } catch (Exception e) {
      logger.error("build state execution failed", e);
    }
    return null;
  }

  public RestResponse<K8sTaskExecutionResponse> executeK8sCILiteEngineTask(
      LiteEngineTaskStepInfo liteEngineTaskStepInfo, Ambiance ambiance) {
    K8PodDetails k8PodDetails = (K8PodDetails) executionSweepingOutputResolver.resolve(
        ambiance, SweepingOutputRefObject.builder().name(ContextElement.podDetails).build());
    final String namespace = k8PodDetails.getNamespace();
    final String clusterName = k8PodDetails.getClusterName();

    try {
      PodSetupInfo podSetupInfo = getPodSetupInfo((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo());

      Set<String> publishStepConnectorIdentifier =
          ((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getBuildJobEnvInfo()).getPublishStepConnectorIdentifier();

      // TODO Use k8 connector from element input
      return SafeHttpCall.execute(managerCIResource.createK8PodTask(clusterName,
          liteEngineTaskStepInfo.getGitConnectorIdentifier(), liteEngineTaskStepInfo.getBranchName(),
          getPodParams(podSetupInfo, namespace, liteEngineTaskStepInfo, publishStepConnectorIdentifier,
              liteEngineTaskStepInfo.isUsePVC())));
    } catch (Exception e) {
      logger.error("lite engine task state execution failed", e);
    }
    return null;
  }

  public CIK8PodParams<CIK8ContainerParams> getPodParams(PodSetupInfo podSetupInfo, String namespace,
      LiteEngineTaskStepInfo liteEngineTaskStepInfo, Set<String> publishStepConnectorIdentifier, boolean usePVC) {
    Map<String, String> map = new HashMap<>();
    map.put(STEP_EXEC, MOUNT_PATH);
    List<String> ports = podSetupInfo.getPodSetupParams()
                             .getContainerDefinitionInfos()
                             .stream()
                             .map(ContainerDefinitionInfo::getPorts)
                             .filter(EmptyPredicate::isNotEmpty)
                             .flatMap(Collection::stream)
                             .map(Object::toString)
                             .collect(Collectors.toList());

    // user input container with custom entry point
    List<CIK8ContainerParams> containerParams =
        podSetupInfo.getPodSetupParams()
            .getContainerDefinitionInfos()
            .stream()
            .map(containerDefinitionInfo
                -> createCIK8ContainerParams(containerDefinitionInfo, map, liteEngineTaskStepInfo, ports))
            .collect(toList());

    CIK8ContainerParamsBuilder addOnCik8ContainerParamsBuilder =
        InternalContainerParamsProvider.getContainerParams(ADDON_CONTAINER);

    addOnCik8ContainerParamsBuilder.containerSecrets(
        ContainerSecrets.builder()
            .publishArtifactEncryptedValues(getPublishArtifactEncryptedValues(publishStepConnectorIdentifier))
            .build());
    // include addon container
    containerParams.add(addOnCik8ContainerParamsBuilder.build());

    List<PVCParams> pvcParams = new ArrayList<>();
    if (usePVC) {
      pvcParams = asList(podSetupInfo.getPvcParams());
    }
    return CIK8PodParams.<CIK8ContainerParams>builder()
        .name(podSetupInfo.getName())
        .namespace(namespace)
        .stepExecVolumeName(STEP_EXEC)
        .stepExecWorkingDir(STEP_EXEC_WORKING_DIR)
        .containerParamsList(containerParams)
        .pvcParamList(pvcParams)
        .initContainerParamsList(Collections.singletonList(
            InternalContainerParamsProvider.getContainerParams(LITE_ENGINE_CONTAINER).build()))
        .build();
  }

  private CIK8ContainerParams createCIK8ContainerParams(ContainerDefinitionInfo containerDefinitionInfo,
      Map<String, String> volumeToMountPath, LiteEngineTaskStepInfo liteEngineTaskStepInfo, List<String> ports) {
    List<String> commands = liteEngineTaskUtils.getLiteEngineCommand();
    List<String> args;
    if (containerDefinitionInfo.getContainerType() == CIContainerType.STEP_EXECUTOR
        && containerDefinitionInfo.isMainLiteEngine()) {
      String serializedLiteEngineStepInfo = getSerializedLiteEngineStepInfo(liteEngineTaskStepInfo);
      args = liteEngineTaskUtils.getMainLiteEngineArguments(serializedLiteEngineStepInfo, ports);
    } else {
      args = liteEngineTaskUtils.getWorkerLiteEngineArguments(
          containerDefinitionInfo.getPorts()
              .stream()
              .findFirst()
              .orElseThrow(() -> new InvalidArgumentsException("ports can not be empty for worker container"))
              .toString());
    }

    return CIK8ContainerParams.builder()
        .name(containerDefinitionInfo.getName())
        .containerResourceParams(containerDefinitionInfo.getContainerResourceParams())
        .containerType(CIContainerType.STEP_EXECUTOR)
        .envVars(getCIExecutorEnvVariables(containerDefinitionInfo))
        .containerSecrets(
            ContainerSecrets.builder().encryptedSecrets(getSecretEnvVars(containerDefinitionInfo)).build())
        .commands(commands)
        .ports(containerDefinitionInfo.getPorts())
        .args(args)
        .imageDetailsWithConnector(
            ImageDetailsWithConnector.builder()
                .imageDetails(containerDefinitionInfo.getContainerImageDetails().getImageDetails())
                .connectorName(containerDefinitionInfo.getContainerImageDetails().getConnectorIdentifier())
                .build())
        .volumeToMountPath(volumeToMountPath)
        .build();
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
  private Map<String, String> getCIExecutorEnvVariables(ContainerDefinitionInfo containerDefinitionInfo) {
    Map<String, String> envVars = new HashMap<>();
    if (isNotEmpty(containerDefinitionInfo.getEnvVars())) {
      envVars.putAll(containerDefinitionInfo.getEnvVars()); // Put customer input env variables
    }
    // Put Harness internal env variable like that of minio
    envVars.put(ENDPOINT_MINIO_VARIABLE, ENDPOINT_MINIO_VARIABLE_VALUE);
    envVars.put(BUCKET_MINIO_VARIABLE, BUCKET_MINIO_VARIABLE_VALUE);
    envVars.put(DELEGATE_SERVICE_TOKEN_VARIABLE, serviceTokenUtils.getServiceToken());
    envVars.put(DELEGATE_SERVICE_ENDPOINT_VARIABLE, DELEGATE_SERVICE_ENDPOINT_VARIABLE_VALUE);
    envVars.put(DELEGATE_SERVICE_ID_VARIABLE, DELEGATE_SERVICE_ID_VARIABLE_VALUE);
    return envVars;
  }
}
