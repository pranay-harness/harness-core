package software.wings.delegatetasks;

import software.wings.service.intfc.verification.CVActivityLogService;

public interface DelegateCVActivityLogService {
  Logger getLogger(String accountId, String cvConfigId, long dataCollectionMinute, String stateExecutionId,
      String suffix, long... suffixTimestampParams);

  interface Logger extends CVActivityLogService.Logger {}
}
