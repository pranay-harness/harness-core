package software.wings.delegatetasks.cv;

import retrofit2.Call;
import software.wings.delegatetasks.DelegateCVActivityLogService.Logger;
import software.wings.service.impl.ThirdPartyApiCallLog;

import java.util.Map;

public interface DataCollectionExecutionContext {
  ThirdPartyApiCallLog createApiCallLog();
  void saveThirdPartyApiCallLog(ThirdPartyApiCallLog thirdPartyApiCallLog);
  Logger getActivityLogger();
  <T> T executeRequest(String thirdPartyApiCallTitle, Call<T> request, Map<String, String> patternsToMaskInCallLog);
  <T> T executeRequest(String thirdPartyApiCallTitle, Call<T> request);
}
