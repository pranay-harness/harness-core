package software.wings.service.impl.infra;

import org.hibernate.validator.constraints.NotEmpty;

public interface InfraDownloadService {
  String PROD_SERVICE_ACCOUNT = "PROD_SERVICE_ACC";
  String QA_SERVICE_ACCOUNT = "QA_SERVICE_ACC";
  String DELEGATE_JAR = "delegate.jar";
  String WATCHER_JAR = "watcher.jar";
  String DEFAULT_ERROR_STRING = "ERROR_GETTING_DATA";

  String getDownloadUrlForDelegate(@NotEmpty String version);

  String getDownloadUrlForWatcher(@NotEmpty String version);
}
