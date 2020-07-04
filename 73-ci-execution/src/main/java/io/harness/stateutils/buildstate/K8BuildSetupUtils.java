package io.harness.stateutils.buildstate;

import static io.harness.common.CIExecutionConstants.SETUP_TASK_ARGS;
import static io.harness.common.CIExecutionConstants.SH_COMMAND;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.stateutils.buildstate.providers.InternalContainerParamsProvider.ContainerKind.ADDON_CONTAINER;
import static io.harness.stateutils.buildstate.providers.InternalContainerParamsProvider.ContainerKind.LITE_ENGINE_CONTAINER;
import static java.util.stream.Collectors.toList;
import static software.wings.common.CICommonPodConstants.MOUNT_PATH;
import static software.wings.common.CICommonPodConstants.STEP_EXEC;
import static software.wings.common.CICommonPodConstants.STEP_EXEC_WORKING_DIR;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ambiance.Ambiance;
import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.environment.pod.PodSetupInfo;
import io.harness.beans.steps.stepinfo.BuildEnvSetupStepInfo;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.sweepingoutputs.ContextElement;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.exception.InvalidRequestException;
import io.harness.managerclient.ManagerCIResource;
import io.harness.network.SafeHttpCall;
import io.harness.references.SweepingOutputRefObject;
import io.harness.rest.RestResponse;
import io.harness.security.encryption.EncryptableSettingWithEncryptionDetails;
import io.harness.stateutils.buildstate.providers.InternalContainerParamsProvider;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.ci.pod.CIContainerType;
import software.wings.beans.ci.pod.CIK8ContainerParams;
import software.wings.beans.ci.pod.CIK8PodParams;
import software.wings.beans.ci.pod.ImageDetailsWithConnector;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

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
  @Inject private LiteEngineTaskUtils liteEngineTaskUtils;
  @Inject private EngineExpressionService engineExpressionService;
  @Inject ExecutionSweepingOutputService executionSweepingOutputResolver;

  public RestResponse<K8sTaskExecutionResponse> executeCISetupTask(
      BuildEnvSetupStepInfo buildEnvSetupStepInfo, Ambiance ambiance) {
    try {
      K8PodDetails k8PodDetails = (K8PodDetails) executionSweepingOutputResolver.resolve(
          ambiance, SweepingOutputRefObject.builder().name(ContextElement.podDetails).build());

      final String namespace = k8PodDetails.getNamespace();
      final String clusterName = k8PodDetails.getClusterName();
      PodSetupInfo podSetupInfo =
          getPodSetupInfo((K8BuildJobEnvInfo) buildEnvSetupStepInfo.getSetupEnv().getBuildJobEnvInfo());
      // TODO Use k8 connector from element input
      return SafeHttpCall.execute(managerCIResource.createK8PodTask(clusterName,
          buildEnvSetupStepInfo.getSetupEnv().getGitConnectorIdentifier(),
          buildEnvSetupStepInfo.getSetupEnv().getBranchName(),
          getPodParams(podSetupInfo, namespace, SH_COMMAND, Collections.singletonList(SETUP_TASK_ARGS),
              ((K8BuildJobEnvInfo) buildEnvSetupStepInfo.getSetupEnv().getBuildJobEnvInfo())
                  .getPublishStepConnectorIdentifier())));

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
      PodSetupInfo podSetupInfo =
          getPodSetupInfo((K8BuildJobEnvInfo) liteEngineTaskStepInfo.getEnvSetup().getBuildJobEnvInfo());

      List<String> command = liteEngineTaskUtils.getLiteEngineCommand();
      List<String> arguments = liteEngineTaskUtils.getLiteEngineArguments(liteEngineTaskStepInfo);

      // TODO Use k8 connector from element input
      return SafeHttpCall.execute(managerCIResource.createK8PodTask(clusterName,
          liteEngineTaskStepInfo.getEnvSetup().getGitConnectorIdentifier(),
          liteEngineTaskStepInfo.getEnvSetup().getBranchName(),
          getPodParams(podSetupInfo, namespace, command, arguments, Collections.emptySet())));
    } catch (Exception e) {
      logger.error("lite engine task state execution failed", e);
    }
    return null;
  }

  public CIK8PodParams<CIK8ContainerParams> getPodParams(PodSetupInfo podSetupInfo, String namespace,
      List<String> commands, List<String> args, Set<String> publishStepConnectorIdentifier) {
    Map<String, String> map = new HashMap<>();
    map.put(STEP_EXEC, MOUNT_PATH);

    // user input container with custom entrypoint
    List<CIK8ContainerParams> containerParams =
        podSetupInfo.getPodSetupParams()
            .getContainerDefinitionInfos()
            .stream()
            .map(containerDefinitionInfo
                -> CIK8ContainerParams.builder()
                       .name(containerDefinitionInfo.getName())
                       .containerResourceParams(containerDefinitionInfo.getContainerResourceParams())
                       .containerType(CIContainerType.STEP_EXECUTOR)
                       .commands(commands)
                       .args(args)
                       .imageDetailsWithConnector(
                           ImageDetailsWithConnector.builder()
                               .imageDetails(containerDefinitionInfo.getContainerImageDetails().getImageDetails())
                               .connectorName(
                                   containerDefinitionInfo.getContainerImageDetails().getConnectorIdentifier())
                               .build())
                       .volumeToMountPath(map)
                       .build())
            .collect(toList());

    CIK8ContainerParams addOnCik8ContainerParams = InternalContainerParamsProvider.getContainerParams(ADDON_CONTAINER);
    Map<String, EncryptableSettingWithEncryptionDetails> publishArtifactEncryptedValues = null;

    if (isNotEmpty(publishStepConnectorIdentifier)) {
      publishArtifactEncryptedValues = new HashMap<>();
      // TODO Harsh Fetch connector encrypted values once connector APIs will be ready
      for (String connectorIdentifier : publishStepConnectorIdentifier) {
        publishArtifactEncryptedValues.put(connectorIdentifier, null);
      }
    }

    addOnCik8ContainerParams.setPublishArtifactEncryptedValues(publishArtifactEncryptedValues);
    // include addon container
    containerParams.add(addOnCik8ContainerParams);

    return CIK8PodParams.<CIK8ContainerParams>builder()
        .name(podSetupInfo.getName())
        .namespace(namespace)
        .stepExecVolumeName(STEP_EXEC)
        .stepExecWorkingDir(STEP_EXEC_WORKING_DIR)
        .containerParamsList(containerParams)
        .initContainerParamsList(
            Collections.singletonList(InternalContainerParamsProvider.getContainerParams(LITE_ENGINE_CONTAINER)))
        .build();
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
}
