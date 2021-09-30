package software.wings.service.intfc.logz;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.TaskType;
import software.wings.beans.config.LogzConfig;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.elk.ElkLogFetchRequest;

import java.io.IOException;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 8/21/17.
 */
@TargetModule(HarnessModule._910_DELEGATE_SERVICE_DRIVER)
public interface LogzDelegateService {
  @DelegateTaskType(TaskType.LOGZ_CONFIGURATION_VALIDATE_TASK)
  boolean validateConfig(@NotNull LogzConfig logzConfig, List<EncryptedDataDetail> encryptedDataDetails);

  @DelegateTaskType(TaskType.LOGZ_GET_HOST_RECORDS)
  Object search(@NotNull LogzConfig logzConfig, List<EncryptedDataDetail> encryptedDataDetails,
      ElkLogFetchRequest logFetchRequest, ThirdPartyApiCallLog apiCallLog) throws IOException;

  @DelegateTaskType(TaskType.LOGZ_GET_LOG_SAMPLE)
  Object getLogSample(LogzConfig logzConfig, List<EncryptedDataDetail> encryptedDataDetails) throws IOException;
}
