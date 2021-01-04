package software.wings.delegatetasks;

import software.wings.beans.Log;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.verification.CVActivityLog;

import javax.validation.Valid;

/**
 * Created by peeyushaggarwal on 1/9/17.
 */
public interface DelegateLogService {
  void save(String accountId, @Valid Log logObject);
  void save(String accountId, @Valid ThirdPartyApiCallLog thirdPartyApiCallLog);
  void save(String accountId, CVActivityLog cvActivityLog);
  void registerLogSanitizer(LogSanitizer sanitizer);
  void unregisterLogSanitizer(LogSanitizer sanitizer);
}
