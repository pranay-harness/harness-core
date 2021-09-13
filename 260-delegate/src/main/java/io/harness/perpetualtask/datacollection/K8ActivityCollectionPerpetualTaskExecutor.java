package io.harness.perpetualtask.datacollection;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.cvng.CVNGRequestExecutor;
import io.harness.cvng.beans.CVDataCollectionInfo;
import io.harness.cvng.beans.activity.KubernetesActivityDTO;
import io.harness.cvng.beans.activity.KubernetesActivityDTO.KubernetesEventType;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.service.KubernetesActivitiesStoreService;
import io.harness.grpc.utils.AnyUtils;
import io.harness.k8s.apiclient.ApiClientFactory;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.logging.AutoLogContext;
import io.harness.perpetualtask.PerpetualTaskExecutionParams;
import io.harness.perpetualtask.PerpetualTaskExecutor;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskLogContext;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.datacollection.changeintel.BaseChangeHandler;
import io.harness.perpetualtask.k8s.informer.handlers.V1DeploymentHandler;
import io.harness.perpetualtask.k8s.watch.K8sWatchServiceDelegate.WatcherGroup;
import io.harness.serializer.KryoSerializer;
import io.harness.verificationclient.CVNextGenServiceClient;

import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.cvng.K8InfoDataService;

import com.google.inject.Inject;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1Event;
import io.kubernetes.client.openapi.models.V1EventList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.CallGeneratorParams;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class K8ActivityCollectionPerpetualTaskExecutor implements PerpetualTaskExecutor {
  private final Map<String, WatcherGroup> watchMap = new ConcurrentHashMap<>();
  @Inject private K8InfoDataService k8InfoDataService;

  @Inject private DelegateLogService delegateLogService;
  @Inject private KryoSerializer kryoSerializer;

  @Inject private ApiClientFactory apiClientFactory;
  @Inject private KubernetesActivitiesStoreService kubernetesActivitiesStoreService;
  @Inject private CVNGRequestExecutor cvngRequestExecutor;
  @Inject private CVNextGenServiceClient cvNextGenServiceClient;

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    try (AutoLogContext ignore1 = new PerpetualTaskLogContext(taskId.getId(), OVERRIDE_ERROR)) {
      K8ActivityCollectionPerpetualTaskParams taskParams =
          AnyUtils.unpack(params.getCustomizedParams(), K8ActivityCollectionPerpetualTaskParams.class);
      log.info("Executing watcher task for change source ID: {}", taskParams.getDataCollectionWorkerId());
      watchMap.computeIfAbsent(taskId.getId(), id -> {
        CVDataCollectionInfo dataCollectionInfo =
            (CVDataCollectionInfo) kryoSerializer.asObject(taskParams.getDataCollectionInfo().toByteArray());
        log.info("for {} DataCollectionInfo {} ", taskParams.getDataCollectionWorkerId(), dataCollectionInfo);
        KubernetesClusterConfigDTO kubernetesClusterConfig =
            (KubernetesClusterConfigDTO) dataCollectionInfo.getConnectorConfigDTO();
        KubernetesConfig kubernetesConfig = k8InfoDataService.getDecryptedKubernetesConfig(
            kubernetesClusterConfig, dataCollectionInfo.getEncryptedDataDetails());
        SharedInformerFactory factory = new SharedInformerFactory();

        //        KubernetesActivitySourceDTO activitySourceDTO =
        //            getActivitySourceDTO(taskParams.getAccountId(), taskParams.getDataCollectionWorkerId());
        //        log.info("for {} got the activity source as {}", taskParams.getDataCollectionWorkerId(),
        //        activitySourceDTO);

        ApiClient apiClient = apiClientFactory.getClient(kubernetesConfig).setVerifyingSsl(false);
        CoreV1Api coreV1Api = new CoreV1Api(apiClient);
        AppsV1Api appsV1Api = new AppsV1Api(apiClient);

        SharedIndexInformer<V1Pod> nodeInformer =
            factory.sharedIndexInformerFor((CallGeneratorParams callGeneratorParams)
                                               -> coreV1Api.listPodForAllNamespacesCall(null, null, null, null, null,
                                                   null, callGeneratorParams.resourceVersion,
                                                   callGeneratorParams.timeoutSeconds, callGeneratorParams.watch, null),
                V1Pod.class, V1PodList.class);
        nodeInformer.addEventHandler(new BaseChangeHandler());

        factory.startAllRegisteredInformers();
        return WatcherGroup.builder().watchId(id).sharedInformerFactory(factory).build();
      });

      return PerpetualTaskResponse.builder().responseCode(200).responseMessage("success").build();
    }
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    try (AutoLogContext ignore1 = new PerpetualTaskLogContext(taskId.getId(), OVERRIDE_ERROR)) {
      String watchId = taskId.getId();
      if (watchMap.get(watchId) == null) {
        return false;
      }
      log.info("Stopping the watch with id {}", watchId);
      watchMap.computeIfPresent(watchId, (id, eventWatcher) -> {
        eventWatcher.close();
        return null;
      });
      watchMap.remove(watchId);
      return true;
    }
  }

  private KubernetesActivitySourceDTO getActivitySourceDTO(String accountId, String dataCollectionWorkerId) {
    return cvngRequestExecutor
        .executeWithRetry(cvNextGenServiceClient.getKubernetesActivitySourceDTO(accountId, dataCollectionWorkerId))
        .getResource();
  }
}
