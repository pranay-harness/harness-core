package io.harness.perpetualtask;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.network.SafeHttpCall.execute;
import static io.harness.state.StateConstants.DEFAULT_STEADY_STATE_TIMEOUT;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;
import static javax.servlet.http.HttpServletResponse.SC_OK;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.instancesync.InstanceSyncPerpetualTaskResponse;
import io.harness.delegate.beans.instancesync.K8sInstanceSyncPerpetualTaskResponse;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.delegate.beans.instancesync.mapper.K8sPodToServiceInstanceInfoMapper;
import io.harness.delegate.task.k8s.ContainerDeploymentDelegateBaseHelper;
import io.harness.delegate.task.k8s.K8sDeploymentReleaseData;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.InvalidRequestException;
import io.harness.grpc.utils.AnyUtils;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.perpetualtask.instancesync.K8sDeploymentRelease;
import io.harness.perpetualtask.instancesync.K8sInstanceSyncPerpetualTaskParams;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class K8sInstanceSyncPerpetualTaskExecutor implements PerpetualTaskExecutor {
  @Inject private KryoSerializer kryoSerializer;
  @Inject private ContainerDeploymentDelegateBaseHelper containerBaseHelper;
  @Inject private K8sTaskHelperBase k8sTaskHelperBase;
  @Inject private DelegateAgentManagerClient delegateAgentManagerClient;

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    log.info("Running the K8s InstanceSync perpetual task executor for task id: {}", taskId);
    K8sInstanceSyncPerpetualTaskParams taskParams =
        AnyUtils.unpack(params.getCustomizedParams(), K8sInstanceSyncPerpetualTaskParams.class);
    return executeK8sInstanceSyncTask(taskId, taskParams);
  }

  private PerpetualTaskResponse executeK8sInstanceSyncTask(
      PerpetualTaskId taskId, K8sInstanceSyncPerpetualTaskParams taskParams) {
    List<K8sDeploymentReleaseData> deploymentReleaseDataList = getK8sDeploymentReleaseData(taskParams);

    List<ServerInstanceInfo> serverInstanceInfos =
        deploymentReleaseDataList.stream()
            .map(this::preparePodDetailsRequestData)
            .flatMap(Collection::stream)
            .collect(collectingAndThen(
                toCollection(() -> new TreeSet<>(comparing(K8sDeploymentReleaseDataInternal::getNamespace))),
                ArrayList::new))
            .stream()
            .map(this::getServerInstanceInfoList)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

    K8sInstanceSyncPerpetualTaskResponse k8sInstanceSyncTaskResponse =
        K8sInstanceSyncPerpetualTaskResponse.builder().serverInstanceDetails(serverInstanceInfos).build();

    publishInstanceSyncResult(taskId, taskParams.getAccountId(), k8sInstanceSyncTaskResponse);

    return PerpetualTaskResponse.builder().responseCode(SC_OK).responseMessage("success").build();
  }

  private List<ServerInstanceInfo> getServerInstanceInfoList(K8sDeploymentReleaseDataInternal releaseDataInternal) {
    long timeoutMillis = K8sTaskHelperBase.getTimeoutMillisFromMinutes(DEFAULT_STEADY_STATE_TIMEOUT);
    try {
      List<K8sPod> k8sPodList = k8sTaskHelperBase.getPodDetails(releaseDataInternal.kubernetesConfig,
          releaseDataInternal.getNamespace(), releaseDataInternal.releaseName, timeoutMillis);
      return K8sPodToServiceInstanceInfoMapper.toServerInstanceInfoList(k8sPodList);
    } catch (Exception e) {
      throw new InvalidRequestException("Unable to get list of server instances");
    }
  }

  private List<K8sDeploymentReleaseDataInternal> preparePodDetailsRequestData(K8sDeploymentReleaseData releaseData) {
    containerBaseHelper.decryptK8sInfraDelegateConfig(releaseData.getK8sInfraDelegateConfig());
    KubernetesConfig kubernetesConfig =
        containerBaseHelper.createKubernetesConfig(releaseData.getK8sInfraDelegateConfig());
    LinkedHashSet<String> namespaces = releaseData.getNamespaces();
    String releaseName = releaseData.getReleaseName();
    return namespaces.stream()
        .map(namespace
            -> K8sDeploymentReleaseDataInternal.builder()
                   .kubernetesConfig(kubernetesConfig)
                   .namespace(namespace)
                   .releaseName(releaseName)
                   .build())
        .collect(Collectors.toList());
  }

  private List<K8sDeploymentReleaseData> getK8sDeploymentReleaseData(K8sInstanceSyncPerpetualTaskParams taskParams) {
    return taskParams.getK8SDeploymentReleaseListList()
        .stream()
        .map(this::toK8sDeploymentReleaseData)
        .collect(Collectors.toList());
  }

  private K8sDeploymentReleaseData toK8sDeploymentReleaseData(K8sDeploymentRelease k8SDeploymentRelease) {
    return K8sDeploymentReleaseData.builder()
        .releaseName(k8SDeploymentRelease.getReleaseName())
        .namespaces(new LinkedHashSet<>(k8SDeploymentRelease.getNamespacesList().stream().collect(Collectors.toSet())))
        .k8sInfraDelegateConfig((K8sInfraDelegateConfig) kryoSerializer.asObject(
            k8SDeploymentRelease.getK8SInfraDelegateConfig().toByteArray()))
        .build();
  }

  private void publishInstanceSyncResult(
      PerpetualTaskId taskId, String accountId, InstanceSyncPerpetualTaskResponse responseData) {
    try {
      execute(delegateAgentManagerClient.processInstanceSyncNGResult(taskId.getId(), accountId, responseData));
    } catch (Exception e) {
      log.error(String.format("Failed to publish K8s instance sync result PerpetualTaskId [%s], accountId [%s]",
                    taskId.getId(), accountId),
          e);
    }
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    return false;
  }

  @Data
  @Builder
  static class K8sDeploymentReleaseDataInternal {
    KubernetesConfig kubernetesConfig;
    String namespace;
    String releaseName;
  }
}
