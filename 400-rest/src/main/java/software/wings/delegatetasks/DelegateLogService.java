package software.wings.delegatetasks;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.cvng.beans.cvnglog.CVNGLogDTO;

import software.wings.beans.Log;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.verification.CVActivityLog;

import javax.validation.Valid;

@TargetModule(Module._930_DELEGATE_TASKS)
public interface DelegateLogService {
  void save(String accountId, @Valid Log logObject);
  void save(String accountId, @Valid ThirdPartyApiCallLog thirdPartyApiCallLog);
  void save(String accountId, CVActivityLog cvActivityLog);
  void registerLogSanitizer(LogSanitizer sanitizer);
  void unregisterLogSanitizer(LogSanitizer sanitizer);
  void save(String accountId, CVNGLogDTO cvngLogDTO);
}
