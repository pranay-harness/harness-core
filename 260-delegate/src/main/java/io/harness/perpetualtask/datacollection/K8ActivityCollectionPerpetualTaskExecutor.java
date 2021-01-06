package io.harness.perpetualtask.datacollection;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.cvng.beans.K8ActivityDataCollectionInfo;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.beans.activity.KubernetesActivityDTO;
import io.harness.cvng.beans.activity.KubernetesActivityDTO.KubernetesEventType;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.service.KubernetesActivitiesStoreService;
import io.harness.delegate.task.k8s.K8sYamlToDelegateDTOMapper;
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
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.serializer.KryoSerializer;

import software.wings.delegatetasks.DelegateLogService;

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
public class K8ActivityCollectionPerpetualTaskExecutor implements PerpetualTaskExecutor {
  private final Map<String, WatcherGroup> watchMap = new ConcurrentHashMap<>();
  @Inject private SecretDecryptionService secretDecryptionService;

  @Inject private DelegateLogService delegateLogService;
  @Inject private KryoSerializer kryoSerializer;

  @Inject private K8sYamlToDelegateDTOMapper k8sYamlToDelegateDTOMapper;
  @Inject private ApiClientFactory apiClientFactory;
  @Inject private KubernetesActivitiesStoreService kubernetesActivitiesStoreService;

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    try (AutoLogContext ignore1 = new PerpetualTaskLogContext(taskId.getId(), OVERRIDE_ERROR)) {
      K8ActivityCollectionPerpetualTaskParams taskParams =
          AnyUtils.unpack(params.getCustomizedParams(), K8ActivityCollectionPerpetualTaskParams.class);
      String activitySourceConfigId = taskParams.getActivitySourceConfigId();
      log.info("Executing for !! activitySourceId: {}", activitySourceConfigId);
      watchMap.computeIfAbsent(taskId.getId(), id -> {
        K8ActivityDataCollectionInfo dataCollectionInfo =
            (K8ActivityDataCollectionInfo) kryoSerializer.asObject(taskParams.getDataCollectionInfo().toByteArray());
        log.info("DataCollectionInfo {} ", dataCollectionInfo);
        KubernetesClusterConfigDTO kubernetesClusterConfig =
            (KubernetesClusterConfigDTO) dataCollectionInfo.getConnectorConfigDTO();
        KubernetesAuthCredentialDTO kubernetesCredentialAuth =
            ((KubernetesClusterDetailsDTO) kubernetesClusterConfig.getCredential().getConfig())
                .getAuth()
                .getCredentials();
        secretDecryptionService.decrypt(kubernetesCredentialAuth, dataCollectionInfo.getEncryptedDataDetails());
        SharedInformerFactory factory = new SharedInformerFactory();
        KubernetesActivitySourceDTO activitySourceDTO =
            (KubernetesActivitySourceDTO) dataCollectionInfo.getActivitySourceDTO();
        KubernetesConfig kubernetesConfig =
            k8sYamlToDelegateDTOMapper.createKubernetesConfigFromClusterConfig(kubernetesClusterConfig, null);
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
                          .message(v1Event.getMessage())
                          .eventDetails(v1Event.toString())
                          .activitySourceConfigId(activitySourceConfigId)
                          .name(v1Event.getInvolvedObject().getUid())
                          .activityStartTime(v1Event.getFirstTimestamp().getMillis())
                          .activityEndTime(v1Event.getLastTimestamp().getMillis())
                          .kubernetesActivityType(ActivityType.INFRASTRUCTURE)
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
}
