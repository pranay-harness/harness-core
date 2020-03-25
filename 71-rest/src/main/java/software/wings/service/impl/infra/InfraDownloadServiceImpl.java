package software.wings.service.impl.infra;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.logging.v2.LoggingScopes;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.environment.SystemEnvironment;
import io.harness.logging.AccessTokenBean;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import software.wings.beans.FeatureName;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.utils.CdnStorageUrlGenerator;
import software.wings.utils.GcsUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
@Slf4j
public class InfraDownloadServiceImpl implements InfraDownloadService {
  private static final String DOWNLOAD_SERVICE_ACCOUNT_ENV_VAR = "SERVICE_ACC";
  private static final String DELEGATE_JAR = "delegate.jar";
  private static final String BUILDS_PATH = "/builds/";

  private static final String LOGGING_SERVICE_ACCOUNT_ENV_VAR = "LOGGING_SERVICE_ACC";

  static final String DEFAULT_ERROR_STRING = "ERROR_GETTING_DATA";
  private static final String ENV_ENV_VAR = "ENV";

  @Inject private GcsUtils gcsUtil;
  @Inject private SystemEnvironment sysenv;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private CdnStorageUrlGenerator cdnStorageUrlGenerator;

  private final Map<String, String> serviceAccountCache = new HashMap<>();

  private LoadingCache<String, AccessTokenBean> accessTokenCache =
      CacheBuilder.newBuilder()
          .maximumSize(1)
          .expireAfterWrite(50, TimeUnit.MINUTES)
          .build(new CacheLoader<String, AccessTokenBean>() {
            @Override
            public AccessTokenBean load(String key) {
              String serviceAccountJson = getServiceAccountJson(key);
              if (isEmpty(serviceAccountJson)) {
                throw new InvalidInfraException("No logging service account available");
              }

              try {
                GoogleCredential credential =
                    GoogleCredential.fromStream(IOUtils.toInputStream(serviceAccountJson, defaultCharset()))
                        .createScoped(singletonList(LoggingScopes.LOGGING_WRITE));

                if (!credential.refreshToken()) {
                  throw new InvalidInfraException("Failed to refresh token");
                }

                return AccessTokenBean.builder()
                    .projectId(credential.getServiceAccountProjectId())
                    .tokenValue(credential.getAccessToken())
                    .expirationTimeMillis(credential.getExpirationTimeMilliseconds())
                    .build();

              } catch (Exception e) {
                throw new InvalidInfraException("Error getting logging token", e);
              }
            }
          });

  @Override
  public String getDownloadUrlForDelegate(String version, String envString, String accountId) {
    if (isEmpty(envString)) {
      envString = getEnv();
    }

    if (featureFlagService.isEnabled(FeatureName.USE_CDN_FOR_STORAGE_FILES, accountId)) {
      return cdnStorageUrlGenerator.getDelegateJarUrl(version);
    } else {
      String serviceAccountJson = getServiceAccountJson(DOWNLOAD_SERVICE_ACCOUNT_ENV_VAR);
      if (isNotBlank(serviceAccountJson)) {
        try {
          return getGcsUtil().getSignedUrlForServiceAccount(
              "/harness-" + envString + "-delegates" + BUILDS_PATH + version + "/" + DELEGATE_JAR, serviceAccountJson,
              3600L, accountId);
        } catch (Exception e) {
          logger.warn("Failed to get downloadUrlForDelegate for version=" + version + ", env=" + envString, e);
        }
      }
    }

    return DEFAULT_ERROR_STRING;
  }

  @Override
  public String getDownloadUrlForDelegate(String version, String accountId) {
    String env = getEnv();
    return getDownloadUrlForDelegate(version, env, accountId);
  }

  @Override
  public AccessTokenBean getStackdriverLoggingToken() {
    if (isBlank(sysenv.get(LOGGING_SERVICE_ACCOUNT_ENV_VAR))) {
      return null;
    }

    try {
      return accessTokenCache.get(LOGGING_SERVICE_ACCOUNT_ENV_VAR);
    } catch (Exception e) {
      logger.error("Failed to get logging token", e);
    }
    return null;
  }

  @Override
  public String getCdnWatcherMetaDataFileUrl() {
    return cdnStorageUrlGenerator.getWatcherMetaDataFileUrl(getEnv());
  }

  @Override
  public String getCdnWatcherBaseUrl() {
    return cdnStorageUrlGenerator.getWatcherJarBaseUrl();
  }

  protected String getEnv() {
    return Optional.ofNullable(sysenv.get(ENV_ENV_VAR)).orElse("ci");
  }

  private String getServiceAccountJson(String serviceAccountEnvVarName) {
    if (serviceAccountCache.containsKey(serviceAccountEnvVarName)) {
      return serviceAccountCache.get(serviceAccountEnvVarName);
    }

    String serviceAccountPath = sysenv.get(serviceAccountEnvVarName);
    if (isEmpty(serviceAccountPath)) {
      String msg = String.format(
          "Environment variable [%s] containing path to the service account not found", serviceAccountEnvVarName);
      logger.error(msg);
      return null;
    }

    File file = new File(serviceAccountPath);
    if (!file.exists()) {
      String msg = String.format("Service account file not found at [%s]", serviceAccountPath);
      logger.error(msg);
      return null;
    }

    try {
      String serviceAccountFileContents = FileUtils.readFileToString(file, UTF_8);
      serviceAccountCache.put(serviceAccountEnvVarName, serviceAccountFileContents);
      return serviceAccountFileContents;
    } catch (Exception e) {
      String msg = String.format("Error reading service account from %s", serviceAccountPath);
      throw new InvalidInfraException(msg, e);
    }
  }

  private GcsUtils getGcsUtil() {
    return gcsUtil;
  }
}
