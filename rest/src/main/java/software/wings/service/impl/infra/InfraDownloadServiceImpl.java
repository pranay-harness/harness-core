package software.wings.service.impl.infra;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.exception.WingsException;
import software.wings.utils.GcsUtil;

import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class InfraDownloadServiceImpl implements InfraDownloadService {
  private static final Logger logger = LoggerFactory.getLogger(InfraDownloadServiceImpl.class);
  final String BUILDS_PATH = "/builds/";

  /**
   * Buckets for watcher / delegate every version
   */
  private static final String CI_DELEGATE_BUCKET = "harness-ci-delegates";
  private static final String CI_WATCHER_BUCKET = "harness-ci-watchers";
  private static final String QA_DELEGATE_BUCKET = "harness-qa-delegates";
  private static final String QA_WATCHER_BUCKET = "harness-qa-watchers";
  private static final String PROD_DELEGATE_BUCKET = "harness-prod-delegates";
  private static final String PROD_WATCHER_BUCKET = "harness-prod-watchers";
  private static final String STAGE_DELEGATE_BUCKET = "harness-stage-delegates";
  private static final String STAGE_WATCHER_BUCKET = "harness-stage-watchers";
  private static final String ON_PREM_DELEGATE_BUCKET = "harness-on-prem-delegates";
  private static final String ON_PREM_WATCHER_BUCKET = "harness-on-prem-watchers";
  @Inject private GcsUtil gcsUtil;

  @Override
  public String getDownloadUrlForDelegate(String version, String envString) {
    HarnessEnv env;
    if (isEmpty(envString)) {
      env = getEnv();
    } else {
      env = HarnessEnv.valueOf(envString);
    }
    try {
      switch (env) {
        case CI:
          return getGcsUtil().getSignedUrlForServiceAccount(
              "/" + CI_DELEGATE_BUCKET + BUILDS_PATH + version + "/" + DELEGATE_JAR,
              getServiceAccountJson(QA_SERVICE_ACCOUNT), 600L);
        case QA:
          return getGcsUtil().getSignedUrlForServiceAccount(
              "/" + QA_DELEGATE_BUCKET + BUILDS_PATH + version + "/" + DELEGATE_JAR,
              getServiceAccountJson(QA_SERVICE_ACCOUNT), 600L);
        case PROD:
          return getGcsUtil().getSignedUrlForServiceAccount(
              "/" + PROD_DELEGATE_BUCKET + BUILDS_PATH + version + "/" + DELEGATE_JAR,
              getServiceAccountJson(PROD_SERVICE_ACCOUNT), 600L);
        case STAGE:
          return getGcsUtil().getSignedUrlForServiceAccount(
              "/" + STAGE_DELEGATE_BUCKET + BUILDS_PATH + version + "/" + DELEGATE_JAR,
              getServiceAccountJson(PROD_SERVICE_ACCOUNT), 600L);
        case ON_PREM:
          return getGcsUtil().getSignedUrlForServiceAccount(
              "/" + ON_PREM_DELEGATE_BUCKET + BUILDS_PATH + version + "/" + DELEGATE_JAR,
              getServiceAccountJson(PROD_SERVICE_ACCOUNT), 600L);
        default:
          throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION, "Unsupported env found, env=" + env);
      }

    } catch (Exception e) {
      logger.warn("Failed to get downloadUrlForDelegate for version=" + version + ", env=" + env, e);
    }
    return DEFAULT_ERROR_STRING;
  }

  @Override
  public String getDownloadUrlForWatcher(String version, String envString) {
    HarnessEnv env;
    if (isEmpty(envString)) {
      env = getEnv();
    } else {
      env = HarnessEnv.valueOf(envString);
    }
    try {
      switch (env) {
        case CI:
          return getGcsUtil().getSignedUrlForServiceAccount(
              "/" + CI_WATCHER_BUCKET + BUILDS_PATH + version + "/" + WATCHER_JAR,
              getServiceAccountJson(QA_SERVICE_ACCOUNT), 600L);
        case QA:
          return getGcsUtil().getSignedUrlForServiceAccount(
              "/" + QA_WATCHER_BUCKET + BUILDS_PATH + version + "/" + WATCHER_JAR,
              getServiceAccountJson(QA_SERVICE_ACCOUNT), 600L);
        case PROD:
          return getGcsUtil().getSignedUrlForServiceAccount(
              "/" + PROD_WATCHER_BUCKET + BUILDS_PATH + version + "/" + WATCHER_JAR,
              getServiceAccountJson(PROD_SERVICE_ACCOUNT), 600L);
        case STAGE:
          return getGcsUtil().getSignedUrlForServiceAccount(
              "/" + STAGE_WATCHER_BUCKET + BUILDS_PATH + version + "/" + WATCHER_JAR,
              getServiceAccountJson(PROD_SERVICE_ACCOUNT), 600L);
        case ON_PREM:
          return getGcsUtil().getSignedUrlForServiceAccount(
              "/" + ON_PREM_WATCHER_BUCKET + BUILDS_PATH + version + "/" + WATCHER_JAR,
              getServiceAccountJson(PROD_SERVICE_ACCOUNT), 600L);
        default:
          throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION, "Unsupported env found, env=" + env);
      }

    } catch (Exception e) {
      logger.warn("Failed to get downloadUrlForDelegate for version=" + version + ", env=" + env, e);
    }
    return DEFAULT_ERROR_STRING;
  }

  @Override
  public String getDownloadUrlForWatcher(String version) {
    HarnessEnv env = getEnv();
    return getDownloadUrlForWatcher(version, env.toString());
  }

  @Override
  public String getDownloadUrlForDelegate(String version) {
    HarnessEnv env = getEnv();
    return getDownloadUrlForDelegate(version, env.toString());
  }

  protected HarnessEnv getEnv() {
    HarnessEnv env = HarnessEnv.CI;
    final String enviornment = System.getenv().get("ENV");
    if (enviornment != null) {
      if (enviornment.equals("stage")) {
        env = HarnessEnv.STAGE;
      } else if (enviornment.equals("prod")) {
        env = HarnessEnv.PROD;
      } else if (enviornment.equals("on-prem")) {
        env = HarnessEnv.ON_PREM;
      } else if (enviornment.equals("qa")) {
        env = HarnessEnv.QA;
      } else if (enviornment.equals("ci")) {
        env = HarnessEnv.CI;
      }
    }
    return env;
  }

  protected String getServiceAccountJson(String serviceAccount) {
    String serviceAccountJson = System.getenv().get(serviceAccount);
    if (isEmpty(serviceAccountJson)) {
      throw new WingsException(ErrorCode.INVALID_INFRA_CONFIGURATION,
          "No Service Account configuration discovered for serviceAccount=" + serviceAccount);
    }
    return serviceAccountJson;
  }

  public GcsUtil getGcsUtil() {
    return gcsUtil;
  }
}
