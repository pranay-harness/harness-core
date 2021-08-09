package software.wings.delegatetasks.cv;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.delegatetasks.DelegateCVActivityLogService.Logger;
import software.wings.service.impl.ThirdPartyApiCallLog;

import java.util.Map;
import retrofit2.Call;
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public interface DataCollectionExecutionContext {
  ThirdPartyApiCallLog createApiCallLog();
  void saveThirdPartyApiCallLog(ThirdPartyApiCallLog thirdPartyApiCallLog);
  Logger getActivityLogger();
  <T> T executeRequest(String thirdPartyApiCallTitle, Call<T> request, Map<String, String> patternsToMaskInCallLog);
  <T> T executeRequest(String thirdPartyApiCallTitle, Call<T> request);
}
