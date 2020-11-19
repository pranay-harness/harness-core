package io.harness.cvng.perpetualtask;

import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.Durations;

import io.harness.beans.DecryptableEntity;
import io.harness.cvng.beans.CVDataCollectionInfo;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.cvng.beans.K8ActivityDataCollectionInfo;
import io.harness.cvng.beans.KubernetesActivitySourceDTO;
import io.harness.cvng.beans.KubernetesActivitySourceDTO.KubernetesActivitySourceDTOKeys;
import io.harness.delegate.Capability;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.govern.Switch;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskExecutionBundle;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams;
import io.harness.perpetualtask.datacollection.K8ActivityCollectionPerpetualTaskParams;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import io.kubernetes.client.openapi.ApiException;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.SyncTaskContext;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.delegatetasks.cvng.K8InfoDataService;
import software.wings.service.intfc.cvng.CVNGDataCollectionDelegateService;
import software.wings.service.intfc.security.NGSecretService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CVDataCollectionTaskServiceImpl implements CVDataCollectionTaskService {
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private NGSecretService ngSecretService;
  @Inject private DelegateProxyFactory delegateProxyFactory;

  @Override
  public String create(
      String accountId, String orgIdentifier, String projectIdentifier, DataCollectionConnectorBundle bundle) {
    bundle.getParams().put("accountId", accountId);
    String taskType;
    byte[] executionBundle;
    switch (bundle.getDataCollectionType()) {
      case CV:
        taskType = PerpetualTaskType.DATA_COLLECTION_TASK;
        executionBundle = createCVExecutionBundle(accountId, orgIdentifier, projectIdentifier, bundle);
        break;
      case KUBERNETES:
        taskType = PerpetualTaskType.K8_ACTIVITY_COLLECTION_TASK;
        executionBundle = createK8ExecutionBundle(accountId, orgIdentifier, projectIdentifier, bundle);
        break;
      default:
        throw new IllegalStateException("Invalid type " + bundle.getDataCollectionType());
    }
    PerpetualTaskClientContext clientContext = PerpetualTaskClientContext.builder()
                                                   .clientId(bundle.getParams().get("dataCollectionWorkerId"))
                                                   .executionBundle(executionBundle)
                                                   .build();
    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(1))
                                         .setTimeout(Durations.fromHours(3))
                                         .build();
    return perpetualTaskService.createTask(taskType, accountId, clientContext, schedule, false, "");
  }

  private byte[] createCVExecutionBundle(
      String accountId, String orgIdentifier, String projectIdentifier, DataCollectionConnectorBundle bundle) {
    List<EncryptedDataDetail> encryptedDataDetailList =
        getEncryptedDataDetail(accountId, orgIdentifier, projectIdentifier, bundle);
    CVDataCollectionInfo cvDataCollectionInfo = CVDataCollectionInfo.builder()
                                                    .connectorConfigDTO(bundle.getConnectorDTO().getConnectorConfig())
                                                    .encryptedDataDetails(encryptedDataDetailList)
                                                    .dataCollectionType(bundle.getDataCollectionType())
                                                    .build();
    String dataCollectionWorkedId = bundle.getParams().get("dataCollectionWorkerId");
    DataCollectionPerpetualTaskParams params =
        DataCollectionPerpetualTaskParams.newBuilder()
            .setAccountId(accountId)
            .setDataCollectionInfo(ByteString.copyFrom(kryoSerializer.asBytes(cvDataCollectionInfo)))
            .setDataCollectionWorkerId(dataCollectionWorkedId)
            .build();

    Any perpetualTaskPack = Any.pack(params);
    List<ExecutionCapability> executionCapabilities = Collections.emptyList();

    PerpetualTaskExecutionBundle perpetualTaskExecutionBundle =
        createPerpetualTaskExecutionBundle(cvDataCollectionInfo, perpetualTaskPack, executionCapabilities);
    return perpetualTaskExecutionBundle.toByteArray();
  }

  private byte[] createK8ExecutionBundle(
      String accountId, String orgIdentifier, String projectIdentifier, DataCollectionConnectorBundle bundle) {
    List<EncryptedDataDetail> encryptedDataDetailList =
        getEncryptedDataDetail(accountId, orgIdentifier, projectIdentifier, bundle);
    K8ActivityDataCollectionInfo k8ActivityDataCollectionInfo =
        K8ActivityDataCollectionInfo.builder()
            .connectorConfigDTO(bundle.getConnectorDTO().getConnectorConfig())
            .encryptedDataDetails(encryptedDataDetailList)
            .dataCollectionType(bundle.getDataCollectionType())
            .activitySourceDTO(KubernetesActivitySourceDTO.builder()
                                   .namespace(bundle.getParams().get(KubernetesActivitySourceDTOKeys.namespace))
                                   .clusterName(bundle.getParams().get(KubernetesActivitySourceDTOKeys.clusterName))
                                   .workloadName(bundle.getParams().get(KubernetesActivitySourceDTOKeys.workloadName))
                                   .build())
            .build();
    List<ExecutionCapability> executionCapabilities = Collections.emptyList();
    K8ActivityCollectionPerpetualTaskParams params =
        K8ActivityCollectionPerpetualTaskParams.newBuilder()
            .setAccountId(accountId)
            .setActivitySourceConfigId(bundle.getParams().get("dataCollectionWorkerId"))
            .setDataCollectionInfo(ByteString.copyFrom(kryoSerializer.asBytes(k8ActivityDataCollectionInfo)))
            .build();
    Any perpetualTaskPack = Any.pack(params);
    PerpetualTaskExecutionBundle perpetualTaskExecutionBundle =
        createPerpetualTaskExecutionBundle(k8ActivityDataCollectionInfo, perpetualTaskPack, executionCapabilities);
    return perpetualTaskExecutionBundle.toByteArray();
  }

  @NotNull
  private PerpetualTaskExecutionBundle createPerpetualTaskExecutionBundle(CVDataCollectionInfo cvDataCollectionInfo,
      Any perpetualTaskPack, List<ExecutionCapability> executionCapabilities) {
    if (cvDataCollectionInfo.getConnectorConfigDTO() instanceof ExecutionCapabilityDemander) {
      executionCapabilities = ((ExecutionCapabilityDemander) cvDataCollectionInfo.getConnectorConfigDTO())
                                  .fetchRequiredExecutionCapabilities();
    }

    PerpetualTaskExecutionBundle.Builder builder = PerpetualTaskExecutionBundle.newBuilder();
    executionCapabilities.forEach(executionCapability
        -> builder
               .addCapabilities(
                   Capability.newBuilder()
                       .setKryoCapability(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(executionCapability)))
                       .build())
               .build());
    return builder.setTaskParams(perpetualTaskPack).build();
  }

  private List<EncryptedDataDetail> getEncryptedDataDetail(
      String accountId, String orgIdentifier, String projectIdentifier, DataCollectionConnectorBundle bundle) {
    NGAccess basicNGAccessObject = BaseNGAccess.builder()
                                       .accountIdentifier(accountId)
                                       .projectIdentifier(projectIdentifier)
                                       .orgIdentifier(orgIdentifier)
                                       .build();
    switch (bundle.getDataCollectionType()) {
      case CV:
        return ngSecretService.getEncryptionDetails(basicNGAccessObject,
            bundle.getConnectorDTO().getConnectorConfig() instanceof DecryptableEntity
                ? (DecryptableEntity) bundle.getConnectorDTO().getConnectorConfig()
                : null);
      case KUBERNETES:
        KubernetesClusterConfigDTO kubernetesClusterConfigDTO =
            (KubernetesClusterConfigDTO) bundle.getConnectorDTO().getConnectorConfig();
        return ngSecretService.getEncryptionDetails(basicNGAccessObject,
            ((KubernetesClusterDetailsDTO) kubernetesClusterConfigDTO.getCredential().getConfig())
                .getAuth()
                .getCredentials());
      default:
        Switch.unhandled(bundle.getDataCollectionType());
        throw new IllegalStateException("invalid type " + bundle.getDataCollectionType());
    }
  }

  @Override
  public void delete(String accountId, String taskId) {
    perpetualTaskService.deleteTask(accountId, taskId);
  }

  @Override
  public String getDataCollectionResult(
      String accountId, String orgIdentifier, String projectIdentifier, DataCollectionRequest dataCollectionRequest) {
    NGAccess basicNGAccessObject = BaseNGAccess.builder()
                                       .accountIdentifier(accountId)
                                       .orgIdentifier(orgIdentifier)
                                       .projectIdentifier(projectIdentifier)
                                       .build();
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    if (dataCollectionRequest.getConnectorConfigDTO() instanceof DecryptableEntity) {
      encryptedDataDetails = ngSecretService.getEncryptionDetails(
          basicNGAccessObject, (DecryptableEntity) dataCollectionRequest.getConnectorConfigDTO());
    }
    SyncTaskContext taskContext = getSyncTaskContext(accountId);
    return delegateProxyFactory.get(CVNGDataCollectionDelegateService.class, taskContext)
        .getDataCollectionResult(accountId, dataCollectionRequest, encryptedDataDetails);
  }
  private SyncTaskContext getSyncTaskContext(String accountId) {
    return SyncTaskContext.builder()
        .accountId(accountId)
        .appId(GLOBAL_APP_ID)
        .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
        .build();
  }
  public List<String> getNamespaces(String accountId, String orgIdentifier, String projectIdentifier, String filter,
      DataCollectionConnectorBundle bundle) throws ApiException {
    List<EncryptedDataDetail> encryptedDataDetails =
        getEncryptedDataDetail(accountId, orgIdentifier, projectIdentifier, bundle);
    SyncTaskContext syncTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();
    return delegateProxyFactory.get(K8InfoDataService.class, syncTaskContext)
        .getNameSpaces(bundle, encryptedDataDetails, filter);
  }

  @Override
  public List<String> getWorkloads(String accountId, String orgIdentifier, String projectIdentifier, String namespace,
      String filter, DataCollectionConnectorBundle bundle) throws ApiException {
    List<EncryptedDataDetail> encryptedDataDetails =
        getEncryptedDataDetail(accountId, orgIdentifier, projectIdentifier, bundle);
    SyncTaskContext syncTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();
    return delegateProxyFactory.get(K8InfoDataService.class, syncTaskContext)
        .getWorkloads(namespace, bundle, encryptedDataDetails, filter);
  }
}
