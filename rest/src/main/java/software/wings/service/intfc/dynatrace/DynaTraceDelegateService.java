package software.wings.service.intfc.dynatrace;

import software.wings.beans.DynaTraceConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.impl.dynatrace.DynaTraceMetricDataRequest;
import software.wings.service.impl.dynatrace.DynaTraceMetricDataResponse;

import java.io.IOException;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * Created by rsingh on 01/29/18.
 */
public interface DynaTraceDelegateService {
  @DelegateTaskType(TaskType.DYNA_TRACE_VALIDATE_CONFIGURATION_TASK)
  boolean validateConfig(@NotNull DynaTraceConfig dynaTraceConfig) throws IOException;

  DynaTraceMetricDataResponse fetchMetricData(@NotNull DynaTraceConfig dynaTraceConfig,
      @NotNull DynaTraceMetricDataRequest dataRequest, @NotNull List<EncryptedDataDetail> encryptedDataDetails,
      ThirdPartyApiCallLog apiCallLog) throws IOException;
}
