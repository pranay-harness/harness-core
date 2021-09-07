package software.wings.service.impl.infra;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AccessTokenBean;

import org.hibernate.validator.constraints.NotEmpty;

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
