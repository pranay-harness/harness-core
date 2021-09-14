/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Event;
import io.kubernetes.client.openapi.models.V1EventList;
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
      log.info("Executing for !! activitySourceId: {}", taskParams.getDataCollectionWorkerId());
      watchMap.computeIfAbsent(taskId.getId(), id -> {
        CVDataCollectionInfo dataCollectionInfo =
            (CVDataCollectionInfo) kryoSerializer.asObject(taskParams.getDataCollectionInfo().toByteArray());
        log.info("for {} DataCollectionInfo {} ", taskParams.getDataCollectionWorkerId(), dataCollectionInfo);
        KubernetesClusterConfigDTO kubernetesClusterConfig =
            (KubernetesClusterConfigDTO) dataCollectionInfo.getConnectorConfigDTO();
        KubernetesConfig kubernetesConfig = k8InfoDataService.getDecryptedKubernetesConfig(
            kubernetesClusterConfig, dataCollectionInfo.getEncryptedDataDetails());
        SharedInformerFactory factory = new SharedInformerFactory();
        KubernetesActivitySourceDTO activitySourceDTO =
            getActivitySourceDTO(taskParams.getAccountId(), taskParams.getDataCollectionWorkerId());
        log.info("for {} got the activity source as {}", taskParams.getDataCollectionWorkerId(), activitySourceDTO);
        ApiClient apiClient = apiClientFactory.getClient(kubernetesConfig).setVerifyingSsl(false);
        CoreV1Api coreV1Api = new CoreV1Api(apiClient);
        SharedIndexInformer<V1Event> nodeInformer = factory.sharedIndexInformerFor(
            (CallGeneratorParams generatorParams)
                -> coreV1Api.listEventForAllNamespacesCall(null, null, null, null, null, null,
                    generatorParams.resourceVersion, generatorParams.timeoutSeconds, generatorParams.watch, null),
            V1Event.class, V1EventList.class);
        nodeInformer.addEventHandler(new ResourceEventHandler<V1Event>() {
          @Override
          public void onAdd(V1Event v1Event) {
            String namespace = v1Event.getInvolvedObject().getNamespace();
            String workLoadName = v1Event.getInvolvedObject().getName();

            activitySourceDTO.getActivitySourceConfigs().forEach(activitySourceConfig -> {
              if ((isEmpty(activitySourceConfig.getNamespaceRegex())
                      && activitySourceConfig.getNamespace().equals(namespace))
                  || (isNotEmpty(activitySourceConfig.getNamespaceRegex())
                      && Pattern.compile(activitySourceConfig.getNamespaceRegex()).matcher(namespace).matches())) {
                if (workLoadName.contains(activitySourceConfig.getWorkloadName())) {
                  kubernetesActivitiesStoreService.save(taskParams.getAccountId(),
                      KubernetesActivityDTO.builder()
                          .namespace(namespace)
                          .workloadName(activitySourceConfig.getWorkloadName())
                          .kind(v1Event.getInvolvedObject().getKind())
                          .reason(v1Event.getReason())
                          .message(v1Event.getMessage())
                          .eventJson(v1Event.toString())
                          .activitySourceConfigId(activitySourceDTO.getUuid())
                          .name(v1Event.getInvolvedObject().getUid())
                          .activityStartTime(v1Event.getFirstTimestamp().getMillis())
                          .activityEndTime(v1Event.getLastTimestamp().getMillis())
                          .eventType(KubernetesEventType.valueOf(v1Event.getType()))
                          .serviceIdentifier(activitySourceConfig.getServiceIdentifier())
                          .environmentIdentifier(activitySourceConfig.getEnvIdentifier())
                          .build());
                }
              }
            });
          }

          @Override
          public void onUpdate(V1Event v1Event, V1Event apiType1) {
            // no updates
          }

          @Override
          public void onDelete(V1Event v1Event, boolean b) {
            // no deletes
          }
        });

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
