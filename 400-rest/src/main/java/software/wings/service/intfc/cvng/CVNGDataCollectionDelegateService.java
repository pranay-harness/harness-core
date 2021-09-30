package software.wings.service.intfc.cvng;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.cvng.beans.DataCollectionRequest;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;

import java.util.List;

@TargetModule(HarnessModule._910_DELEGATE_SERVICE_DRIVER)
public interface CVNGDataCollectionDelegateService {
  @DelegateTaskType(TaskType.GET_DATA_COLLECTION_RESULT)
  String getDataCollectionResult(
      String accountId, DataCollectionRequest dataCollectionRequest, List<EncryptedDataDetail> encryptedDataDetails);
}
