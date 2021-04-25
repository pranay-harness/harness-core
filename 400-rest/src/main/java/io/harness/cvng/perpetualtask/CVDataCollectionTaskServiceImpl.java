package io.harness.cvng.perpetualtask;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import io.harness.cvng.beans.CVDataCollectionInfo;
import io.harness.cvng.beans.CVNGPerpetualTaskDTO;
import io.harness.cvng.beans.CVNGPerpetualTaskState;
import io.harness.cvng.beans.CVNGPerpetualTaskUnassignedReason;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.delegate.Capability;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterDetailsDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.capability.EncryptedDataDetailsCapabilityHelper;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.govern.Switch;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskExecutionBundle;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskState;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.PerpetualTaskUnassignedReason;
import io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams;
import io.harness.perpetualtask.datacollection.K8ActivityCollectionPerpetualTaskParams;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;

import software.wings.beans.SyncTaskContext;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.delegatetasks.cvng.K8InfoDataService;
import software.wings.service.intfc.cvng.CVNGDataCollectionDelegateService;
import software.wings.service.intfc.security.NGSecretService;

import com.google.inject.Inject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.Durations;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class CVDataCollectionTaskServiceImpl implements CVDataCollectionTaskService {
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private NGSecretService ngSecretService;
  @Inject private DelegateProxyFactory delegateProxyFactory;

  @Override
  public void resetTask(String accountId, String orgIdentifier, String projectIdentifier, String taskId,
      DataCollectionConnectorBundle bundle) {
    PerpetualTaskExecutionBundle executionBundle =
        createExecutionBundle(accountId, orgIdentifier, projectIdentifier, bundle);
    perpetualTaskService.resetTask(accountId, taskId, executionBundle);
  }

  @Override
  public String create(
      String accountId, String orgIdentifier, String projectIdentifier, DataCollectionConnectorBundle bundle) {
    String taskType;
    PerpetualTaskExecutionBundle executionBundle =
        createExecutionBundle(accountId, orgIdentifier, projectIdentifier, bundle);
    switch (bundle.getDataCollectionType()) {
      case CV:
        taskType = PerpetualTaskType.DATA_COLLECTION_TASK;
        break;
      case KUBERNETES:
        taskType = PerpetualTaskType.K8_ACTIVITY_COLLECTION_TASK;
        break;
      default:
        throw new IllegalStateException("Invalid type " + bundle.getDataCollectionType());
    }
    PerpetualTaskClientContext clientContext = PerpetualTaskClientContext.builder()
                                                   .clientId(bundle.getDataCollectionWorkerId())
                                                   .executionBundle(executionBundle.toByteArray())
                                                   .build();
    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(1))
                                         .setTimeout(Durations.fromHours(3))
                                         .build();
    return perpetualTaskService.createTask(taskType, accountId, clientContext, schedule, false, "");
  }

  private PerpetualTaskExecutionBundle createExecutionBundle(
      String accountId, String orgIdentifier, String projectIdentifier, DataCollectionConnectorBundle bundle) {
    List<EncryptedDataDetail> encryptedDataDetailList =
        getEncryptedDataDetail(accountId, orgIdentifier, projectIdentifier, bundle);
    CVDataCollectionInfo cvDataCollectionInfo = CVDataCollectionInfo.builder()
                                                    .connectorConfigDTO(bundle.getConnectorDTO().getConnectorConfig())
                                                    .encryptedDataDetails(encryptedDataDetailList)
                                                    .dataCollectionType(bundle.getDataCollectionType())
                                                    .build();
    Any perpetualTaskPack;
    switch (bundle.getDataCollectionType()) {
      case CV:
        DataCollectionPerpetualTaskParams params =
            DataCollectionPerpetualTaskParams.newBuilder()
                .setAccountId(accountId)
                .setDataCollectionWorkerId(bundle.getDataCollectionWorkerId())
                .setDataCollectionInfo(ByteString.copyFrom(kryoSerializer.asBytes(cvDataCollectionInfo)))
                .build();
        perpetualTaskPack = Any.pack(params);
        break;
      case KUBERNETES:
        K8ActivityCollectionPerpetualTaskParams k8ActivityCollectionPerpetualTaskParams =
            K8ActivityCollectionPerpetualTaskParams.newBuilder()
                .setAccountId(accountId)
                .setDataCollectionWorkerId(bundle.getDataCollectionWorkerId())
                .setDataCollectionInfo(ByteString.copyFrom(kryoSerializer.asBytes(cvDataCollectionInfo)))
                .build();
        perpetualTaskPack = Any.pack(k8ActivityCollectionPerpetualTaskParams);
        break;
      default:
        throw new IllegalStateException("Invalid type " + bundle.getDataCollectionType());
    }
    List<ExecutionCapability> executionCapabilities =
        EncryptedDataDetailsCapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(
            encryptedDataDetailList, null);
    executionCapabilities.addAll(bundle.fetchRequiredExecutionCapabilities(null));
    return createPerpetualTaskExecutionBundle(perpetualTaskPack, executionCapabilities);
  }

  @NotNull
  private PerpetualTaskExecutionBundle createPerpetualTaskExecutionBundle(
      Any perpetualTaskPack, List<ExecutionCapability> executionCapabilities) {
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
                                       .orgIdentifier(orgIdentifier)
                                       .projectIdentifier(projectIdentifier)
                                       .build();
    switch (bundle.getDataCollectionType()) {
      case CV:
        return ngSecretService.getEncryptionDetails(basicNGAccessObject,
            isNotEmpty(bundle.getConnectorDTO().getConnectorConfig().getDecryptableEntities())
                ? bundle.getConnectorDTO().getConnectorConfig().getDecryptableEntities().get(0)
                : null);
      case KUBERNETES:
        KubernetesClusterConfigDTO kubernetesClusterConfigDTO =
            (KubernetesClusterConfigDTO) bundle.getConnectorDTO().getConnectorConfig();
        KubernetesCredentialDTO credential = kubernetesClusterConfigDTO.getCredential();
        if (!credential.getKubernetesCredentialType().isDecryptable()) {
          return new ArrayList<>();
        }
        return ngSecretService.getEncryptionDetails(
            basicNGAccessObject, ((KubernetesClusterDetailsDTO) credential.getConfig()).getAuth().getCredentials());
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
  public CVNGPerpetualTaskDTO getCVNGPerpetualTaskDTO(String taskId) {
    PerpetualTaskRecord perpetualTaskRecord = perpetualTaskService.getTaskRecord(taskId);
    return CVNGPerpetualTaskDTO.builder()
        .delegateId(perpetualTaskRecord.getDelegateId())
        .accountId(perpetualTaskRecord.getAccountId())
        .cvngPerpetualTaskUnassignedReason(
            mapUnassignedReasonFromPerpetualTaskToCVNG(perpetualTaskRecord.getUnassignedReason()))
        .cvngPerpetualTaskState(mapStateFromPerpetualTaskToCVNG(perpetualTaskRecord.getState()))
        .build();
  }

  private CVNGPerpetualTaskUnassignedReason mapUnassignedReasonFromPerpetualTaskToCVNG(
      PerpetualTaskUnassignedReason perpetualTaskUnassignedReason) {
    switch (perpetualTaskUnassignedReason) {
      case NO_DELEGATE_AVAILABLE:
        return CVNGPerpetualTaskUnassignedReason.NO_DELEGATE_AVAILABLE;
      case NO_DELEGATE_INSTALLED:
        return CVNGPerpetualTaskUnassignedReason.NO_DELEGATE_INSTALLED;
      case NO_ELIGIBLE_DELEGATES:
        return CVNGPerpetualTaskUnassignedReason.NO_ELIGIBLE_DELEGATES;
      default:
        throw new UnknownEnumTypeException("Task Unassigned Reason", String.valueOf(perpetualTaskUnassignedReason));
    }
  }

  private CVNGPerpetualTaskState mapStateFromPerpetualTaskToCVNG(PerpetualTaskState perpetualTaskState) {
    switch (perpetualTaskState) {
      case TASK_ASSIGNED:
        return CVNGPerpetualTaskState.TASK_ASSIGNED;
      case TASK_UNASSIGNED:
        return CVNGPerpetualTaskState.TASK_UNASSIGNED;
      case TASK_PAUSED:
        return CVNGPerpetualTaskState.TASK_PAUSED;
      case TASK_TO_REBALANCE:
        return CVNGPerpetualTaskState.TASK_TO_REBALANCE;
      case NO_DELEGATE_INSTALLED:
      case NO_DELEGATE_AVAILABLE:
      case NO_ELIGIBLE_DELEGATES:
      case TASK_RUN_SUCCEEDED:
      case TASK_RUN_FAILED:
        return null;
      default:
        throw new UnknownEnumTypeException("Perpetual task state", String.valueOf(perpetualTaskState));
    }
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
    if (isNotEmpty(dataCollectionRequest.getConnectorConfigDTO().getDecryptableEntities())) {
      encryptedDataDetails = ngSecretService.getEncryptionDetails(
          basicNGAccessObject, dataCollectionRequest.getConnectorConfigDTO().getDecryptableEntities().get(0));
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
      DataCollectionConnectorBundle bundle) {
    List<EncryptedDataDetail> encryptedDataDetails =
        getEncryptedDataDetail(accountId, orgIdentifier, projectIdentifier, bundle);
    SyncTaskContext syncTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();
    return delegateProxyFactory.get(K8InfoDataService.class, syncTaskContext)
        .getNameSpaces(bundle, encryptedDataDetails, filter);
  }

  @Override
  public List<String> getWorkloads(String accountId, String orgIdentifier, String projectIdentifier, String namespace,
      String filter, DataCollectionConnectorBundle bundle) {
    List<EncryptedDataDetail> encryptedDataDetails =
        getEncryptedDataDetail(accountId, orgIdentifier, projectIdentifier, bundle);
    SyncTaskContext syncTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();
    return delegateProxyFactory.get(K8InfoDataService.class, syncTaskContext)
        .getWorkloads(namespace, bundle, encryptedDataDetails, filter);
  }

  @Override
  public List<String> checkCapabilityToGetEvents(
      String accountId, String orgIdentifier, String projectIdentifier, DataCollectionConnectorBundle bundle) {
    List<EncryptedDataDetail> encryptedDataDetails =
        getEncryptedDataDetail(accountId, orgIdentifier, projectIdentifier, bundle);
    SyncTaskContext syncTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();
    return delegateProxyFactory.get(K8InfoDataService.class, syncTaskContext)
        .checkCapabilityToGetEvents(bundle, encryptedDataDetails);
  }
}
