package io.harness.cvng.perpetualtask;

import com.google.inject.Inject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.Durations;

import io.harness.beans.DecryptableEntity;
import io.harness.cvng.beans.DataCollectionConnectorBundle;
import io.harness.delegate.Capability;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskExecutionBundle;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import software.wings.service.intfc.security.NGSecretService;

import java.util.Collections;
import java.util.List;

public class CVDataCollectionTaskServiceImpl implements CVDataCollectionTaskService {
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private NGSecretService ngSecretService;
  @Override
  public String create(
      String accountId, String orgIdentifier, String projectIdentifier, DataCollectionConnectorBundle bundle) {
    bundle.getParams().put("accountId", accountId);
    NGAccess basicNGAccessObject = BaseNGAccess.builder()
                                       .accountIdentifier(accountId)
                                       .projectIdentifier(projectIdentifier)
                                       .orgIdentifier(orgIdentifier)
                                       .build();
    List<EncryptedDataDetail> encryptedDataDetailList = ngSecretService.getEncryptionDetails(basicNGAccessObject,
        bundle.getConnectorConfigDTO() instanceof DecryptableEntity ? (DecryptableEntity) bundle.getConnectorConfigDTO()
                                                                    : null);

    CVDataCollectionInfo cvDataCollectionInfo = CVDataCollectionInfo.builder()
                                                    .connectorConfigDTO(bundle.getConnectorConfigDTO())
                                                    .encryptedDataDetails(encryptedDataDetailList)
                                                    .build();
    String dataCollectionWorkedId = bundle.getParams().get("dataCollectionWorkerId");
    DataCollectionPerpetualTaskParams params =
        DataCollectionPerpetualTaskParams.newBuilder()
            .setAccountId(accountId)
            .setDataCollectionInfo(ByteString.copyFrom(kryoSerializer.asBytes(cvDataCollectionInfo)))
            .setDataCollectionWorkerId(dataCollectionWorkedId)
            .build();

    List<ExecutionCapability> executionCapabilities = Collections.emptyList();
    if (cvDataCollectionInfo.getConnectorConfigDTO() instanceof ExecutionCapabilityDemander) {
      executionCapabilities = ((ExecutionCapabilityDemander) cvDataCollectionInfo.getConnectorConfigDTO())
                                  .fetchRequiredExecutionCapabilities();
    }

    PerpetualTaskExecutionBundle perpetualTaskExecutionBundle =
        createPerpetualTaskExecutionBundle(params, executionCapabilities);

    byte[] executionBundle = perpetualTaskExecutionBundle.toByteArray();
    PerpetualTaskClientContext clientContext =
        PerpetualTaskClientContext.builder().clientId(dataCollectionWorkedId).executionBundle(executionBundle).build();
    PerpetualTaskSchedule schedule = PerpetualTaskSchedule.newBuilder()
                                         .setInterval(Durations.fromMinutes(1))
                                         .setTimeout(Durations.fromHours(3))
                                         .build();
    return perpetualTaskService.createTask(
        PerpetualTaskType.DATA_COLLECTION_TASK, accountId, clientContext, schedule, false, "");
  }

  @Override
  public void delete(String accountId, String taskId) {
    perpetualTaskService.deleteTask(accountId, taskId);
  }

  private PerpetualTaskExecutionBundle createPerpetualTaskExecutionBundle(
      DataCollectionPerpetualTaskParams params, List<ExecutionCapability> executionCapabilities) {
    PerpetualTaskExecutionBundle.Builder builder = PerpetualTaskExecutionBundle.newBuilder();
    executionCapabilities.forEach(executionCapability
        -> builder
               .addCapabilities(
                   Capability.newBuilder()
                       .setKryoCapability(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(executionCapability)))
                       .build())
               .build());
    return builder.setTaskParams(Any.pack(params)).build();
  }
}
