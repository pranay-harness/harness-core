package software.wings.service.impl.infra;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.exception.WingsException;
import software.wings.utils.GcsUtil;

import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class InfraDownloadServiceImpl implements InfraDownloadService {
  private static final Logger logger = LoggerFactory.getLogger(InfraDownloadServiceImpl.class);

  private static final String BUILDS_PATH = "/builds/";

  @Inject private GcsUtil gcsUtil;

  @Override
  public String getDownloadUrlForDelegate(String version, String envString) {
    if (isEmpty(envString)) {
      envString = getEnv();
    }
    try {
      return getGcsUtil().getSignedUrlForServiceAccount(
          "/harness-" + envString + "-delegates" + BUILDS_PATH + version + "/" + DELEGATE_JAR,
          getServiceAccountJson(envString), 600L);
    } catch (Exception e) {
      logger.warn("Failed to get downloadUrlForDelegate for version=" + version + ", env=" + envString, e);
    }
    return DEFAULT_ERROR_STRING;
  }

  @Override
  public String getDownloadUrlForWatcher(String version, String envString) {
    if (isEmpty(envString)) {
      envString = getEnv();
    }
    try {
      return getGcsUtil().getSignedUrlForServiceAccount(
          "/harness-" + envString + "-watchers" + BUILDS_PATH + version + "/" + WATCHER_JAR,
          getServiceAccountJson(envString), 600L);

    } catch (Exception e) {
      logger.warn("Failed to get downloadUrlForDelegate for version=" + version + ", env=" + envString, e);
    }
    return DEFAULT_ERROR_STRING;
  }

  @Override
  public String getDownloadUrlForWatcher(String version) {
    String env = getEnv();
    return getDownloadUrlForWatcher(version, env);
  }

  @Override
  public String getDownloadUrlForDelegate(String version) {
    String env = getEnv();
    return getDownloadUrlForDelegate(version, env);
  }

  protected String getEnv() {
    return Optional.ofNullable(System.getenv().get("ENV")).orElse("ci");
  }

  protected String getServiceAccountJson(String env) {
    String serviceAccountJson = System.getenv().get(SERVICE_ACCOUNT);

    if (isEmpty(serviceAccountJson)) {
      throw new WingsException(
          ErrorCode.INVALID_INFRA_CONFIGURATION, "No Service Account configuration discovered for env=" + env);
    }
    return serviceAccountJson;
  }

  public GcsUtil getGcsUtil() {
    return gcsUtil;
  }
}
