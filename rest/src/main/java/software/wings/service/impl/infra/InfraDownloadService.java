package software.wings.service.impl.infra;

import org.hibernate.validator.constraints.NotEmpty;

public interface InfraDownloadService {
  String SERVICE_ACCOUNT = "SERVICE_ACC";
  String DELEGATE_JAR = "delegate.jar";
  String WATCHER_JAR = "watcher.jar";
  String DEFAULT_ERROR_STRING = "ERROR_GETTING_DATA";

  String getDownloadUrlForDelegate(@NotEmpty String version, String env);

  String getDownloadUrlForWatcher(@NotEmpty String version, String env);

  String getDownloadUrlForDelegate(@NotEmpty String version);

  String getDownloadUrlForWatcher(@NotEmpty String version);
}
