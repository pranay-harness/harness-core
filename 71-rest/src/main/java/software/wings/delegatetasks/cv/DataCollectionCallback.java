package software.wings.delegatetasks.cv;

import software.wings.delegatetasks.DelegateCVActivityLogService.Logger;
import software.wings.service.impl.ThirdPartyApiCallLog;

public interface DataCollectionCallback {
  ThirdPartyApiCallLog createApiCallLog();
  void saveThirdPartyApiCallLog(ThirdPartyApiCallLog thirdPartyApiCallLog);
  Logger getActivityLogger();
}
