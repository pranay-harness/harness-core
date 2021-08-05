package software.wings.service.impl.infra;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.AccessTokenBean;

import org.hibernate.validator.constraints.NotEmpty;

@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
@OwnedBy(PL)
public interface InfraDownloadService {
  String getDownloadUrlForDelegate(@NotEmpty String version, String env, String accountId);

  String getDownloadUrlForWatcher(@NotEmpty String version, String env, String accountId);

  String getDownloadUrlForDelegate(@NotEmpty String version, String accountId);

  String getDownloadUrlForWatcher(@NotEmpty String version, String accountId);

  AccessTokenBean getStackdriverLoggingToken();

  String getCdnWatcherMetaDataFileUrl();

  String getCdnWatcherBaseUrl();
}
