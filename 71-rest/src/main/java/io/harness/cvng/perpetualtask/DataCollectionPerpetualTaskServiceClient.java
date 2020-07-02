package io.harness.cvng.perpetualtask;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.TaskData;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.perpetualtask.PerpetualTaskServiceClient;
import io.harness.perpetualtask.datacollection.DataCollectionPerpetualTaskParams;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import lombok.extern.slf4j.Slf4j;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DataCollectionPerpetualTaskServiceClient implements PerpetualTaskServiceClient {
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;
  @Inject private KryoSerializer kryoSerializer;
  @Override
  public Message getTaskParams(PerpetualTaskClientContext clientContext) {
    Map<String, String> clientParams = clientContext.getClientParams();
    String accountId = clientParams.get("accountId");
    String cvConfigId = clientParams.get("cvConfigId");
    String connectorId = clientParams.get("connectorId");

    SettingAttribute settingAttribute = settingsService.get(connectorId);
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    if (settingAttribute.getValue() instanceof EncryptableSetting) {
      encryptedDataDetails = secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue());
    }
    CVDataCollectionInfo cvDataCollectionInfo = CVDataCollectionInfo.builder()
                                                    .settingValue(settingAttribute.getValue())
                                                    .encryptedDataDetails(encryptedDataDetails)
                                                    .build();
    ByteString bytes = ByteString.copyFrom(kryoSerializer.asBytes(cvDataCollectionInfo));
    return DataCollectionPerpetualTaskParams.newBuilder()
        .setAccountId(accountId)
        .setCvConfigId(cvConfigId)
        .setDataCollectionInfo(bytes)
        .build();
  }

  @Override
  public void onTaskStateChange(
      String taskId, PerpetualTaskResponse newPerpetualTaskResponse, PerpetualTaskResponse oldPerpetualTaskResponse) {
    // ignore - no state change implementation needed for this.
  }

  @Override
  public DelegateTask getValidationTask(PerpetualTaskClientContext clientContext, String accountId) {
    logger.info("Get validation task {} {}", accountId, clientContext);
    Map<String, String> clientParams = clientContext.getClientParams();
    logger.info("Client params {}", clientParams);
    // TODO: move this to capability framework. For now the validation will always pass.
    return DelegateTask.builder()
        .accountId(accountId)
        .data(TaskData.builder()
                  .async(false)
                  .taskType(TaskType.DATA_COLLECTION_NEXT_GEN_VALIDATION.name())
                  .parameters(new Object[] {"test"})
                  .timeout(TimeUnit.MINUTES.toMillis(1))
                  .build())
        .build();
  }
}
