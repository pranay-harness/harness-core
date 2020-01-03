package software.wings.delegatetasks;

import software.wings.service.intfc.verification.CVActivityLogService;

public interface DelegateCVActivityLogService {
  Logger getLogger(String accountId, String cvConfigId, long dataCollectionMinute, String stateExecutionId,
      String suffix, long... suffixTimestampParams);
  default Logger getLogger(String accountId, String cvConfigId, long dataCollectionMinute, String stateExecutionId) {
    return getLogger(accountId, cvConfigId, dataCollectionMinute, stateExecutionId, "");
  }

  interface Logger extends CVActivityLogService.Logger {}
}
