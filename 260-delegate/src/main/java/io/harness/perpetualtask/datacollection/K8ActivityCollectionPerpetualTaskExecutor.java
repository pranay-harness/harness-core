package io.harness.perpetualtask.datacollection;

import io.harness.cvng.beans.K8ActivityDataCollectionInfo;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.beans.activity.KubernetesActivityDTO;
import io.harness.cvng.beans.activity.KubernetesActivityDTO.KubernetesEventType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.service.KubernetesActivitiesStoreService;
import io.harness.delegate.task.k8s.K8sYamlToDelegateDTOMapper;
import io.harness.grpc.utils.AnyUtils;
import io.harness.k8s.apiclient.ApiClientFactory;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.perpetualtask.PerpetualTaskExecutionParams;
import io.harness.perpetualtask.PerpetualTaskExecutor;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.k8s.watch.K8sWatchServiceDelegate;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class K8ActivityCollectionPerpetualTaskExecutor implements PerpetualTaskExecutor {
  @Inject private SecretDecryptionService secretDecryptionService;

  @Inject private DelegateLogService delegateLogService;
  @Inject private KryoSerializer kryoSerializer;

  @Inject private K8sYamlToDelegateDTOMapper k8sYamlToDelegateDTOMapper;
  @Inject private ApiClientFactory apiClientFactory;
  @Inject private K8ActivityCollectionWatches k8ActivityCollectionWatches;
  @Inject private KubernetesActivitiesStoreService kubernetesActivitiesStoreService;

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    K8ActivityCollectionPerpetualTaskParams taskParams =
        AnyUtils.unpack(params.getCustomizedParams(), K8ActivityCollectionPerpetualTaskParams.class);
    String activitySourceConfigId = taskParams.getActivitySourceConfigId();
    log.info("Executing for !! activitySourceId: {}", activitySourceConfigId);
    k8ActivityCollectionWatches.getWatchMap().computeIfAbsent(activitySourceConfigId, id -> {
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
      dataCollectionInfo.getActivitySourceDTO().getActivitySourceConfigs().forEach(activitySourceConfig -> {
        KubernetesConfig kubernetesConfig = k8sYamlToDelegateDTOMapper.createKubernetesConfigFromClusterConfig(
            kubernetesClusterConfig, activitySourceConfig.getNamespace());
        ApiClient apiClient = apiClientFactory.getClient(kubernetesConfig);
        CoreV1Api coreV1Api = new CoreV1Api(apiClient);
        SharedIndexInformer<V1Event> nodeInformer = factory.sharedIndexInformerFor(
            (CallGeneratorParams generatorParams)
                -> coreV1Api.listNamespacedEventCall(activitySourceConfig.getNamespace(), null, null, null, null, null,
                    null, generatorParams.resourceVersion, generatorParams.timeoutSeconds, generatorParams.watch, null),
            V1Event.class, V1EventList.class);

        nodeInformer.addEventHandler(new ResourceEventHandler<V1Event>() {
          @Override
          public void onAdd(V1Event v1Event) {
            if (v1Event.getInvolvedObject().getName().contains(activitySourceConfig.getWorkloadName())) {
              // don't save kubelet normal events
              if (v1Event.getType().equals(KubernetesEventType.Normal.name())
                  && v1Event.getSource().getComponent().equals("kubelet")) {
                //                return;
              }
              ActivityType activityType;
              switch (v1Event.getSource().getComponent()) {
                case "deployment-controller":
                  activityType = ActivityType.DEPLOYMENT;
                  break;
                case "replicaset-controller":
                  activityType = ActivityType.INFRASTRUCTURE;
                  break;
                default:
                  activityType = ActivityType.OTHER;
                  break;
              }
              kubernetesActivitiesStoreService.save(taskParams.getAccountId(),
                  KubernetesActivityDTO.builder()
                      .message(v1Event.getMessage())
                      .eventDetails(v1Event.toString())
                      .activitySourceConfigId(activitySourceConfigId)
                      .name(v1Event.getInvolvedObject().getUid())
                      .activityStartTime(v1Event.getFirstTimestamp().getMillis())
                      .activityEndTime(v1Event.getLastTimestamp().getMillis())
                      .kubernetesActivityType(activityType)
                      .eventType(KubernetesEventType.valueOf(v1Event.getType()))
                      .serviceIdentifier(activitySourceConfig.getServiceIdentifier())
                      .environmentIdentifier(activitySourceConfig.getEnvIdentifier())
                      .build());
            }
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
      });

      factory.startAllRegisteredInformers();
      return K8sWatchServiceDelegate.WatcherGroup.builder().watchId(id).sharedInformerFactory(factory).build();
    });

    return PerpetualTaskResponse.builder().responseCode(200).responseMessage("success").build();
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    return true;
  }
}
