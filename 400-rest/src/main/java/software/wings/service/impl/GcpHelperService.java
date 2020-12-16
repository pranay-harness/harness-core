package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INVALID_CLOUD_PROVIDER;
import static io.harness.exception.WingsException.USER;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.network.Http;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.JsonUtils;

import software.wings.beans.GcpConfig;
import software.wings.beans.TaskType;
import software.wings.exception.GcbClientException;
import software.wings.service.impl.gcp.GcpCredentialsHelperService;
import software.wings.service.impl.gcp.GcpHttpTransportHelperService;
import software.wings.service.intfc.security.EncryptionService;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.container.Container;
import com.google.api.services.container.ContainerScopes;
import com.google.api.services.logging.v2.Logging;
import com.google.api.services.monitoring.v3.Monitoring;
import com.google.api.services.storage.Storage;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by bzane on 2/22/17
 */
@Singleton
@Slf4j
public class GcpHelperService {
  public static final String LOCATION_DELIMITER = "/";
  public static final String ALL_LOCATIONS = "-";

  private static final int SLEEP_INTERVAL_SECS = 5;
  private static final int TIMEOUT_MINS = 30;
  private static final String GOOGLE_INTERNAL_COMPUTE_METADATA_DEFAULT_TOKEN_URL =
      "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token?scopes=";
  private static final String GOOGLE_META_BASE_URL = "http://metadata.google.internal/computeMetadata";

  @Inject private EncryptionService encryptionService;
  @Inject private GcpHttpTransportHelperService gcpHttpTransportHelperService;
  @Inject private GcpCredentialsHelperService gcpCredentialsHelperService;

  /**
   * Gets a GCP container service.
   *
   * @return the gke container service
   */
  public Container getGkeContainerService(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptedDataDetails, boolean isInstanceSync) {
    try {
      JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
      HttpTransport transport = gcpHttpTransportHelperService.checkIfUseProxyAndGetHttpTransport();
      GoogleCredential credential = getGoogleCredential(gcpConfig, encryptedDataDetails, isInstanceSync);
      return new Container.Builder(transport, jsonFactory, credential).setApplicationName("Harness").build();
    } catch (GeneralSecurityException e) {
      log.error("Security exception getting Google container service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    } catch (IOException e) {
      log.error("Error getting Google container service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    }
  }

  /**
   * Gets a GCS Service
   *
   * @return the gcs storage service
   */
  public Storage getGcsStorageService(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    try {
      JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
      HttpTransport transport = gcpHttpTransportHelperService.checkIfUseProxyAndGetHttpTransport();
      GoogleCredential credential = getGoogleCredential(gcpConfig, encryptedDataDetails, false);
      return new Storage.Builder(transport, jsonFactory, credential).setApplicationName("Harness").build();
    } catch (GeneralSecurityException e) {
      log.error("Security exception getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    } catch (IOException e) {
      log.error("Error getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    }
  }

  public Compute getGCEService(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptedDataDetails, String projectId) {
    try {
      JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
      HttpTransport transport = gcpHttpTransportHelperService.checkIfUseProxyAndGetHttpTransport();
      GoogleCredential credential = getGoogleCredential(gcpConfig, encryptedDataDetails, false);
      return new Compute.Builder(transport, jsonFactory, credential).setApplicationName(projectId).build();
    } catch (GeneralSecurityException e) {
      log.error("Security exception getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    } catch (IOException e) {
      log.error("Error getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    }
  }

  public Monitoring getMonitoringService(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptedDataDetails, String projectId) throws IOException {
    try {
      JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
      HttpTransport transport = gcpHttpTransportHelperService.checkIfUseProxyAndGetHttpTransport();
      GoogleCredential credential = getGoogleCredential(gcpConfig, encryptedDataDetails, false);
      return new Monitoring.Builder(transport, jsonFactory, credential).setApplicationName(projectId).build();
    } catch (GeneralSecurityException e) {
      log.error("Security exception getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    } catch (IOException e) {
      log.error("Error getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    }
  }

  public Logging getLoggingResource(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptedDataDetails, String projectId) {
    try {
      JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
      HttpTransport transport = gcpHttpTransportHelperService.checkIfUseProxyAndGetHttpTransport();
      GoogleCredential credential = getGoogleCredential(gcpConfig, encryptedDataDetails, false);
      return new Logging.Builder(transport, jsonFactory, credential).setApplicationName(projectId).build();
    } catch (GeneralSecurityException e) {
      log.error("Security exception getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    } catch (IOException e) {
      log.error("Error getting Google storage service", e);
      throw new WingsException(INVALID_CLOUD_PROVIDER, USER)
          .addParam("message", "Invalid Google Cloud Platform credentials.");
    }
  }

  public GoogleCredential getGoogleCredential(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptedDataDetails, boolean isInstanceSync) throws IOException {
    if (gcpConfig.isUseDelegate()) {
      return GoogleCredential.getApplicationDefault();
    }
    if (isNotEmpty(encryptedDataDetails)) {
      encryptionService.decrypt(gcpConfig, encryptedDataDetails, isInstanceSync);
    }

    validateServiceAccountKey(gcpConfig);

    return checkIfUseProxyAndGetGoogleCredentials(gcpConfig);
  }

  private GoogleCredential checkIfUseProxyAndGetGoogleCredentials(GcpConfig gcpConfig) throws IOException {
    String tokenUri =
        (String) (JsonUtils.asObject(new String(gcpConfig.getServiceAccountKeyFileContent()), HashMap.class))
            .get("token_uri");
    return Http.getProxyHostName() != null && !Http.shouldUseNonProxy(tokenUri)
        ? gcpCredentialsHelperService.getGoogleCredentialWithProxyConfiguredHttpTransport(gcpConfig)
        : gcpCredentialsHelperService.getGoogleCredentialWithDefaultHttpTransport(gcpConfig);
  }

  private void validateServiceAccountKey(GcpConfig gcpConfig) {
    if (isEmpty(gcpConfig.getServiceAccountKeyFileContent())) {
      throw new InvalidRequestException("Empty service key found. Unable to validate", USER);
    }
  }

  public String getDefaultCredentialsAccessToken(TaskType taskType) {
    OkHttpClient client = Http.getOkHttpClientBuilder()
                              .connectTimeout(10, TimeUnit.SECONDS)
                              .proxy(Http.checkAndGetNonProxyIfApplicable(GOOGLE_META_BASE_URL))
                              .build();
    Request request = new Request.Builder()
                          .header("Metadata-Flavor", "Google")
                          .url(GOOGLE_INTERNAL_COMPUTE_METADATA_DEFAULT_TOKEN_URL + ContainerScopes.CLOUD_PLATFORM)
                          .build();
    try {
      okhttp3.Response response = client.newCall(request).execute();
      if (response.isSuccessful()) {
        log.info(taskType.name() + " - Fetched OAuth2 access token from metadata server");
        return String.join(
            " ", "Bearer", (String) JsonUtils.asObject(response.body().string(), HashMap.class).get("access_token"));
      }
      log.error(taskType.name() + " - Failed to fetch access token from metadata xxxxxxxx " + response);
      throw new GcbClientException("Failed to fetch access token from metadata server");
    } catch (IOException | NullPointerException e) {
      log.error(taskType.name() + " - Failed to get accessToken due to: ", e);
      throw new InvalidRequestException("Can not retrieve accessToken from from cluster meta");
    }
  }

  public String getClusterProjectId(TaskType taskType) {
    OkHttpClient client = Http.getOkHttpClientBuilder()
                              .connectTimeout(10, TimeUnit.SECONDS)
                              .proxy(Http.checkAndGetNonProxyIfApplicable(GOOGLE_META_BASE_URL))
                              .build();
    Request request = new Request.Builder()
                          .header("Metadata-Flavor", "Google")
                          .url("http://metadata.google.internal/computeMetadata/v1/project/project-id")
                          .build();
    try {
      okhttp3.Response response = client.newCall(request).execute();
      String projectId = response.body().string();
      log.info(taskType.name() + " - Fetched projectId from metadata xxxxxxxx " + projectId);
      return projectId;
    } catch (IOException | NullPointerException e) {
      log.error(taskType.name() + " - Failed to get projectId due to: ", e);
      throw new InvalidRequestException("Can not retrieve project-id from from cluster meta");
    }
  }

  /**
   * Gets sleep interval secs.
   *
   * @return the sleep interval secs
   */
  public int getSleepIntervalSecs() {
    return SLEEP_INTERVAL_SECS;
  }

  /**
   * Gets timeout mins.
   *
   * @return the timeout mins
   */
  public int getTimeoutMins() {
    return TIMEOUT_MINS;
  }
}
