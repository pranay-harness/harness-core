package software.wings.service.impl.infra;

import io.harness.logging.AccessTokenBean;
import org.hibernate.validator.constraints.NotEmpty;

public interface InfraDownloadService {
  String getDownloadUrlForDelegate(@NotEmpty String version, String env);

  String getDownloadUrlForWatcher(@NotEmpty String version, String env);

  String getDownloadUrlForDelegate(@NotEmpty String version);

  String getDownloadUrlForWatcher(@NotEmpty String version);

  AccessTokenBean getStackdriverLoggingToken();
}
